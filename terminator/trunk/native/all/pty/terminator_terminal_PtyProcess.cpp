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

void terminator_terminal_PtyProcess::nativeStartProcess(jobjectArray command, jobject descriptor) {
    PtyGenerator ptyGenerator;
    int masterFd = ptyGenerator.openMaster();
    
    JavaStringArrayToStringArray arguments(m_env, command);
    Argv argv(arguments);
    processId = ptyGenerator.forkAndExec(&argv[0]);
    
    slavePtyName = newStringUtf8(ptyGenerator.getSlavePtyName());
    
    fd = masterFd;
#ifdef __CYGWIN__
    (void) descriptor;
#else
    /**
     * This doesn't have the desired effect on Win32,
     * where a "long handle" field which contains a Win32 native handle
     * (contrary to one mention of _open_osfhandle in conjunction with
     * JNI on the interweb).
     * The Win32-compatible back-ends below should work OK on other
     * POSIX implementations.
     * I've been bitten before by not calling read/write in appropriate
     * EINTR loops or similar.
     * I didn't want to compromise the Linux or Mac OS X builds for the
     * sake of an experimental Cygwin build.
     */
    JniField<jint>(m_env, descriptor, "fd", "I") = masterFd;
#endif
}

// The back-end for a Win32-compatible InputStream.
// This absolutely needs to be native code on cygwin because cygwin's syscalls implement the pseudo-terminal behavior.
jint terminator_terminal_PtyProcess::nativeRead(jbyteArray destination, jint arrayOffset, jint desiredLength) {
    std::vector<jbyte> temporary(desiredLength);
    ssize_t bytesTransferred = read(fd.get(), &temporary[0], desiredLength);
    if (bytesTransferred < 0) {
        throw unix_exception("read(" + toString(fd.get()) + ", &array[" + toString(arrayOffset) +"], " + toString(desiredLength) + ") failed");
    }
    if (bytesTransferred == 0) {
        return -1;
    }
    m_env->SetByteArrayRegion(destination, arrayOffset, bytesTransferred, &temporary[0]);
    return bytesTransferred;
}

// The back-end for a Win32-compatible OutputStream.
// This absolutely needs to be native code on cygwin because cygwin's syscalls implement the pseudo-terminal behavior.
void terminator_terminal_PtyProcess::nativeWrite(jint source) {
    char temporary = source;
    ssize_t bytesTransferred = write(fd.get(), &temporary, 1);
    if (bytesTransferred <= 0) {
        throw unix_exception("write(" + toString(fd.get()) + ", &array[0], 1) failed");
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
    int status;
    pid_t result = waitpid(pid, &status, 0);
    if (result < 0) {
        throw unix_exception("waitpid(" + toString(pid) + ", &status, 0) failed");
    }
    
    if (WIFEXITED(status)) {
        exitValue = WEXITSTATUS(status);
        didExitNormally = true;
    }
    if (WIFSIGNALED(status)) {
        exitValue = WTERMSIG(status);
        wasSignaled = true;
        if (WCOREDUMP(status)) {
            didDumpCore = true;
        }
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
    } catch (unix_exception& ex) {
        // We expect not to be able to see all users' processes' fds.
        if (errno != EACCES) {
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
