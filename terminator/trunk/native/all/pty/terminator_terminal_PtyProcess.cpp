#ifdef __CYGWIN__
#include <windows.h>
#endif

#include "terminator_terminal_PtyProcess.h"

#include "DirectoryIterator.h"
#include "JniString.h"
#include "join.h"
#include "PtyGenerator.h"
#include "toString.h"
#include "unix_exception.h"

#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <termios.h>

#include <sys/types.h>
#include <sys/stat.h>
#ifdef __APPLE__ // sysctl.h doesn't exist on Cygwin.
#include <sys/sysctl.h>
#endif
#include <sys/wait.h>

#include <deque>
#include <fstream>
#include <iostream>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

typedef std::vector<std::string> StringArray;

struct JavaStringArrayToStringArray : StringArray {
    JavaStringArrayToStringArray(JNIEnv* env, jobjectArray javaStringArray) {
        int arrayLength = env->GetArrayLength(javaStringArray);
        for (int i = 0; i != arrayLength; ++i) {
            jstring s = static_cast<jstring>(env->GetObjectArrayElement(javaStringArray, i));
            push_back(JniString(env, s));
        }
    }
};

struct Argv : std::vector<char*> {
    // Non-const because execvp is anti-social about const.
    Argv(StringArray& arguments) {
        for (StringArray::iterator it = arguments.begin(); it != arguments.end(); ++it) {
            // We must point to the memory in arguments, not a local.
            std::string& argument = *it;
            push_back(&argument[0]);
        }
        // execvp wants a null-terminated array of pointers to null terminated strings.
        push_back(0);
    }
};

static void waitUntilFdWritable(int fd) {
    int rc;
    do {
        fd_set fds;
        FD_ZERO(&fds);
        FD_SET(fd, &fds);
        rc = ::select(fd + 1, 0, &fds, 0, 0);
    } while (rc == -1 && errno == EINTR);
    if (rc != 1) {
        throw unix_exception("select(" + toString(fd) + ", ...) failed");
    }
}

void terminator_terminal_PtyProcess::nativeStartProcess(jstring javaExecutable, jobjectArray javaArgv, jstring javaWorkingDirectory) {
    PtyGenerator ptyGenerator;
    fd = ptyGenerator.openMaster();
    
    JavaStringArrayToStringArray arguments(m_env, javaArgv);
    Argv argv(arguments);
    std::string executable(JniString(m_env, javaExecutable));
    
    std::string workingDirectory("");
    if (javaWorkingDirectory != 0) {
        workingDirectory = JniString(m_env, javaWorkingDirectory);
    }
    
    weaklyTypedPid = ptyGenerator.forkAndExec("terminator", executable, &argv[0], workingDirectory);
    
    // On Linux, the TIOCSWINSZ ioctl sets the size of the pty (without blocking) even if it hasn't been opened by the child yet.
    // On Mac OS, it silently does nothing, meaning that when the child does open the pty, TIOCGWINSZ reports the wrong size.
    // We work around this by explicitly blocking the parent until the child has opened the pty.
    // We can recognize this on Mac OS by the fact that a write would no longer block.
    // (The fd is writable on Linux even before the child has opened the pty.)
    waitUntilFdWritable(fd.get());
    
    slavePtyName = newStringUtf8(ptyGenerator.getSlavePtyName());
}

jint terminator_terminal_PtyProcess::nativeRead(jbyteArray destination, jint arrayOffset, jint desiredLength) {
    if (fd.get() == -1) {
        throw unix_exception("nativeRead called when fd == -1");
    }
    
    // Zero byte reads worked for me on Cygwin 1.5.25 but let's eliminate the opportunity for crashing in this corner case.
    if (desiredLength == 0) {
        return 0;
    }
    
    jbyte buffer[8192];
    if (desiredLength > jint(sizeof(buffer))) {
        throw std::runtime_error("can't read more than " + toString(sizeof(buffer)) + " bytes at once; desiredLength=" + toString(desiredLength));
    }
    
    ssize_t bytesTransferred;
    do {
        bytesTransferred = ::read(fd.get(), &buffer[0], desiredLength);
    } while (bytesTransferred == -1 && errno == EINTR);
    
    if (bytesTransferred == -1) {
        throw unix_exception("read(" + toString(fd.get()) + ", &buffer[0], " + toString(desiredLength) + ") failed");
    }
    if (bytesTransferred == 0) {
        return -1;
    }
    
    m_env->SetByteArrayRegion(destination, arrayOffset, bytesTransferred, &buffer[0]);
    return bytesTransferred;
}

void terminator_terminal_PtyProcess::nativeWrite(jbyteArray bytes, jint arrayOffset, jint byteCount) {
    if (fd.get() == -1) {
        throw unix_exception("nativeWrite called when fd == -1");
    }
    
    // On Cygwin, on 2006-09-28, attempting a zero-byte write caused the JVM to crash with an EXCEPTION_ACCESS_VIOLATION in a "cygwin1.dll" stack frame.
    // So let's make sure we never do that.
    if (byteCount == 0) {
        return;
    }
    
    // This method isn't critical to performance. It's only native code because it has to be for Cygwin and is convenient elsewhere.
    // GetByteArrayRegion requires less code than GetByteArrayElements/ReleaseByteArrayElements, and measurement shows that the construction of the std::vector is more expensive anyway.
    std::vector<jbyte> buffer(byteCount);
    m_env->GetByteArrayRegion(bytes, arrayOffset, byteCount, &buffer[0]);
    if (m_env->ExceptionCheck()) {
        return;
    }
    
    // POSIX (http://www.opengroup.org/onlinepubs/000095399/functions/write.html) says:
    // 1. we can be interrupted before any bytes are written (n == -1, errno == EINTR).
    // 2. we can be interrupted after some bytes are written (n < requested n).
    size_t offset = 0;
    size_t remainingByteCount = byteCount;
    while (remainingByteCount > 0) {
        ssize_t n = ::write(fd.get(), &buffer[offset], remainingByteCount);
        if (n == -1 && errno != EINTR) {
            // This write failed, and not because we were interrupted before writing anything. Give up.
            break;
        }
        if (n > 0) {
            offset += n;
            remainingByteCount -= n;
        }
    }
    
    if (remainingByteCount != 0) {
        throw unix_exception("write(" + toString(fd.get()) + ", &buffer[" + toString(arrayOffset) + "], " + toString(byteCount) + ") failed");
    }
}

void terminator_terminal_PtyProcess::sendResizeNotification(jobject sizeInChars, jobject sizeInPixels) {
    if (fd.get() == -1) {
        // We shouldn't read or write from a closed pty, but this will happen if the user resizes a window whose child has died.
        // That could just be because they want to read the error message, or because they're fiddling with other tabs.
        return;
    }
    
    struct winsize size;
    size.ws_col = JniField<jint, false>(m_env, sizeInChars, "width", "I").get();
    size.ws_row = JniField<jint, false>(m_env, sizeInChars, "height", "I").get();
    size.ws_xpixel = JniField<jint, false>(m_env, sizeInPixels, "width", "I").get();
    size.ws_ypixel = JniField<jint, false>(m_env, sizeInPixels, "height", "I").get();
    if (ioctl(fd.get(), TIOCSWINSZ, &size) < 0) {
        throw unix_exception("ioctl(" + toString(fd.get()) + ", TIOCSWINSZ, &size) failed");
    }
}

void terminator_terminal_PtyProcess::destroy() {
    pid_t pid = weaklyTypedPid.get();
    int status = killpg(pid, SIGHUP);
    if (status < 0) {
        throw unix_exception("killpg(" + toString(pid) + ", SIGHUP) failed");
    }
}

void terminator_terminal_PtyProcess::nativeWaitFor() {
    // We now have no further use for the fd connecting us to the child,
    // which has probably exited.
    // Even if it hasn't, we're no longer reading its output, which may cause the child to block in the kernel,
    // preventing it from terminating, even if root sends it SIGKILL.
    // If we close the pipe before waiting, then we may let it finish and collect an exit status.
    close(fd.get());
    fd = -1;
    
    pid_t pid = weaklyTypedPid.get();
    
    // Loop until waitpid(2) returns a status or a real error.
    int status = 0;
    errno = 0;
    pid_t result;
    do {
        // Don't block indefinitely, even if the child is still running.
        // At this point we've lost the ability to talk to it.
        result = waitpid(pid, &status, 0);
    } while (result == -1 && errno == EINTR);
    
    // Did something really go wrong?
    if (result == -1) {
        throw unix_exception("waitpid(" + toString(pid) + ", &status, 0) failed");
    }
    
    // We must check "result" to distinguish the case where the child is still running,
    // from the case where the child exited normally.
    if (result == 0) {
        return;
    }
    
    // Tell our Java peer how the process died.
    if (WIFEXITED(status)) {
        exitValue = WEXITSTATUS(status);
        didExitNormally = true;
    }
    if (WIFSIGNALED(status)) {
        exitValue = WTERMSIG(status);
        wasSignaled = true;
#ifdef WCOREDUMP // WCOREDUMP is not POSIX.  The Linux man page recommends this #ifdef.
        if (WCOREDUMP(status)) {
            didDumpCore = true;
        }
#endif
    }
}

#ifdef __APPLE__

// Mac OS doesn't support /proc, but it does have a convenient sysctl(3).

void listProcessesUsingTty(std::deque<std::string>& processNames, std::string ttyFilename) {
    // Which tty?
    struct stat sb;
    if (stat(ttyFilename.c_str(), &sb) != 0) {
        throw unix_exception("stat(" + ttyFilename + ", &sb) failed");
    }
    
    // Fill out our MIB.
    int mib[] = { CTL_KERN, KERN_PROC, KERN_PROC_TTY, sb.st_rdev };
    
    // How much space will we need?
    size_t byteCount = 0;
    if (sysctl(mib, sizeof(mib)/sizeof(int), NULL, &byteCount, NULL, 0) == -1) {
        throw unix_exception("stat(mib, " + toString(sizeof(mib)/sizeof(int)) + ", NULL, &byteCount, NULL, 0) failed");
    }
    
    // Actually get the process information.
    std::vector<char> buffer;
    buffer.resize(byteCount);
    if (sysctl(mib, sizeof(mib)/sizeof(int), &buffer[0], &byteCount, NULL, 0) == -1) {
        throw unix_exception("stat(mib, " + toString(sizeof(mib)/sizeof(int)) + ", &buffer[0], &byteCount, NULL, 0) failed");
    }
    
    // Collect the process names and ids.
    int count = byteCount / sizeof(kinfo_proc);
    kinfo_proc* kp = (kinfo_proc*) &buffer[0];
    for (int i = 0; i < count; ++i) {
        // FIXME: can we easily sort these into "ps -Helf" order?
        processNames.push_back(std::string(kp->kp_proc.p_comm) + "(" + toString(kp->kp_proc.p_pid) + ")");
        ++kp;
    }
}

#else

// Our other platforms (Cygwin, Linux, and Solaris) don't support the particular sysctl(3) parameters we use on Mac OS.
// At one point we used called lsof(1) from Java but that was slow: at best on my work Linux box it took 350ms, but it could easily take more than 1s.
// Users reported times much worse than that. It also turned out that lsof(1) would hang if you had a hung mount, which was obviously unacceptable.
// Experimentation with a Ruby script showed that Ruby could grovel through /proc/*/fd/ in about 40ms on the same Linux box, and wasn't much slower on Cygwin (which is otherwise notoriously slow, and doesn't have an lsof(1) we could have used). Cygwin, Linux, and Solaris all support compatible /proc/<pid>/fd/ directories. (Mac OS only offers an equivalent of /proc/self/fd/, under /dev/fd/.)
// This C++ implementation (measured in PtyProcess to include the JNI cost) gets the result in just under 20ms, and shouldn't be hangable.

static bool isInteger(const std::string& s) {
    return (s.find_first_not_of("0123456789") == std::string::npos);
}

static bool processHasFileOpen(const std::string& pid, const std::string& filename) {
    std::string fdDirectoryName = std::string("/proc/") + pid + "/fd/";
    try {
        std::vector<char> buf;
        // If the link points to a longer name, we'll be able to read more than filename.length() bytes.
        buf.resize(filename.length() + 1);
        
        for (DirectoryIterator it(fdDirectoryName); it.isValid(); ++it) {
            int status = ::readlink((fdDirectoryName + it->getName()).c_str(), &buf[0], buf.size());
            if (status == int(filename.length()) && memcmp(filename.data(), &buf[0], filename.length()) == 0) {
                return true;
            }
        }
    } catch (const unix_exception& ex) {
        // We expect not to be able to see all users' processes' fds.
        // We also expect that some processes might have exited between us seeing their directory and scanning it.
        if (ex.getErrno() != EACCES && ex.getErrno() != ENOENT) {
            // FIXME: Contending for stderr from multiple threads isn't likely to end well.
            fprintf(stderr, "processHasFileOpen error: %s\n", ex.what());
        }
    }
    return false;
}

std::string getProcessName(const std::string& pid) {
    // We used to use "/proc/<pid>/stat", but stopped because Linux truncates the process name there to 15 characters.
    // "/proc/<pid>/cmdline" seems to contain a process' full name.
    // The only thing to be careful of is that the file contains a NUL byte between each argument.
    // std::string will preserve them until they cause trouble higher up, so we remove them here.
    // Solaris 10 doesn't have /proc/<pid>/cmdline or, seemingly, anything as easy to parse.
    std::fstream fin((std::string("/proc/") + pid + "/cmdline").c_str(), std::ios::in);
    std::string processName("(unknown)");
    getline(fin, processName, '\0');
    return processName;
}

void listProcessesUsingTty(std::deque<std::string>& processNames, std::string ttyFilename) {
    for (DirectoryIterator it("/proc"); it.isValid(); ++it) {
        std::string pid(it->getName());
        if (isInteger(pid) && processHasFileOpen(pid, ttyFilename)) {
            processNames.push_back(getProcessName(pid) + "(" + pid + ")");
        }
    }
}

#endif

jstring terminator_terminal_PtyProcess::nativeListProcessesUsingTty() {
    // Say a childless Bash dies with a signal. We'll keep the window open, but the pty is free for reuse.
    // If the user opens another window (reusing the now-free pty) and then does "Show Info" in the original window, they'll see the new window's processes.
    // Guard against this by refusing to list processes if our file descriptor for the original pty is no longer open.
    if (fd.get() == -1) {
        return newStringUtf8("(pty closed)");
    }
    
    std::deque<std::string> processNames;
    std::string ttyFilename(JniString(m_env, slavePtyName.get()));
    
    listProcessesUsingTty(processNames, ttyFilename);
    return newStringUtf8(join(", ", processNames));
}
