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
            JniString jniString(env, static_cast<jstring>(env->GetObjectArrayElement(javaStringArray, i)));
            push_back(jniString.str());
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

void terminator_terminal_PtyProcess::nativeStartProcess(jobjectArray command, jstring javaWorkingDirectory) {
    PtyGenerator ptyGenerator;
    fd = ptyGenerator.openMaster();
    
    JavaStringArrayToStringArray arguments(m_env, command);
    Argv argv(arguments);
    std::string workingDirectoryChars; // Owns the memory for the lifetime of workingDirectory.
    const char* workingDirectory = 0;
    if (javaWorkingDirectory != 0) {
        workingDirectoryChars = JniString(m_env, javaWorkingDirectory).str();
        workingDirectory = workingDirectoryChars.c_str();
    }
    processId = ptyGenerator.forkAndExec(&argv[0], workingDirectory);
    
    // On Linux, the TIOCSWINSZ ioctl sets the size of the pty (without blocking) even if it hasn't been opened by the child yet.
    // On Mac OS, it silently does nothing, meaning that when the child does open the pty, TIOCGWINSZ reports the wrong size.
    // We work around this by explicitly blocking the parent until the child has opened the pty.
    // We can recognize this on Mac OS by the fact that a write would no longer block.
    // (The fd is writable on Linux even before the child has opened the pty.)
    waitUntilFdWritable(fd.get());
    
    slavePtyName = newStringUtf8(ptyGenerator.getSlavePtyName());
}

jint terminator_terminal_PtyProcess::nativeRead(jbyteArray destination, jint arrayOffset, jint desiredLength) {
    std::vector<jbyte> temporary(desiredLength);
    ssize_t bytesTransferred;
    do {
        bytesTransferred = ::read(fd.get(), &temporary[0], desiredLength);
    } while (bytesTransferred == -1 && errno == EINTR);
    
    if (bytesTransferred == -1) {
        throw unix_exception("read(" + toString(fd.get()) + ", &array[" + toString(arrayOffset) +"], " + toString(desiredLength) + ") failed");
    }
    if (bytesTransferred == 0) {
        return -1;
    }
    m_env->SetByteArrayRegion(destination, arrayOffset, bytesTransferred, &temporary[0]);
    return bytesTransferred;
}

void terminator_terminal_PtyProcess::nativeWrite(jbyteArray bytes, jint arrayOffset, jint byteCount) {
    std::vector<jbyte> buffer(byteCount);
    m_env->GetByteArrayRegion(bytes, arrayOffset, byteCount, &buffer[0]);
    ssize_t bytesTransferred;
    do {
        bytesTransferred = ::write(fd.get(), &buffer[0], byteCount);
    } while (bytesTransferred == -1 && errno == EINTR);
    
    if (bytesTransferred <= 0) {
        throw unix_exception("write(" + toString(fd.get()) + ", &buffer[0], " + toString(byteCount) + ") failed");
    }
}

void terminator_terminal_PtyProcess::sendResizeNotification(jobject sizeInChars, jobject sizeInPixels) {
    struct winsize size;
    size.ws_col = JniField<jint>(m_env, sizeInChars, "width", "I").get();
    size.ws_row = JniField<jint>(m_env, sizeInChars, "height", "I").get();
    size.ws_xpixel = JniField<jint>(m_env, sizeInPixels, "width", "I").get();
    size.ws_ypixel = JniField<jint>(m_env, sizeInPixels, "height", "I").get();
    if (ioctl(fd.get(), TIOCSWINSZ, &size) < 0) {
        throw unix_exception("ioctl(" + toString(fd.get()) + ", TIOCSWINSZ, &size) failed");
    }
}

void terminator_terminal_PtyProcess::destroy() {
    pid_t pid = processId.get();
    int status = killpg(pid, SIGHUP);
    if (status < 0) {
        throw unix_exception("killpg(" + toString(pid) + ", SIGHUP) failed");
    }
}

void terminator_terminal_PtyProcess::nativeWaitFor() {
    pid_t pid = processId.get();
    
    // Loop until waitpid(2) returns a status or a real error.
    int status = 0;
    errno = 0;
    pid_t result;
    do {
        result = waitpid(pid, &status, 0);
    } while (result == -1 && errno == EINTR);
    
    // Did something really go wrong?
    if (result == -1) {
        throw unix_exception("waitpid(" + toString(pid) + ", &status, 0) failed");
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
    
    // We now have no further use for the fd connecting us to the (exited) child.
    close(fd.get());
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
    } catch (unix_exception& ex) {
        // We expect not to be able to see all users' processes' fds.
        // We also expect that some processes might have exited between us seeing their directory and scanning it.
        if (ex.getErrno() != EACCES && ex.getErrno() != ENOENT) {
            std::cerr << "processHasFileOpen error: " << ex.what() << std::endl;
        }
    }
    return false;
}

std::string getProcessName(const std::string& pid) {
    // "/proc/self/stat" looks something like this on Linux:
    // 27933 (cat) R 19066 27933 19066 34864 27933 8388608 130 0 0 0 0 0 0 0 16 0 1 0 447453066 2809856 110 4294967295 134512640 134527732 3218535600 3977853053 2097734 0 4 536870912 0 0 0 0 17 1 0 0
    // And like this on Cygwin:
    // 1804 (cat) R 1752 1804 1752 0 -1 0 374 374 0 0 15 0 15 0 8 0 0 0 784431493 958464 377 345
    // FIXME: Solaris doesn't support "/proc/self/stat", but I don't have a machine to test Solaris-specific code on.
    std::fstream fin((std::string("/proc/") + pid + "/stat").c_str(), std::ios::in);
    std::string dummyPid, processName("(unknown)");
    fin >> dummyPid; fin >> processName;
    if (processName.length() > 2) {
        // Strip the parentheses off the process name.
        processName = processName.substr(1, processName.length() - 2);
    }
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
    std::deque<std::string> processNames;
    std::string ttyFilename(JniString(m_env, slavePtyName.get()).str());
    
    listProcessesUsingTty(processNames, ttyFilename);
    return newStringUtf8(join(", ", processNames));
}
