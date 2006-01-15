#ifdef __CYGWIN__
#include <windows.h>
#endif

#include "terminator_terminal_PtyProcess.h"

#include <JniString.h>

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

jstring terminator_terminal_PtyProcess::nativeListProcessesUsingTty() {
#ifndef __APPLE__
    return newStringUtf8("");
#else
    // Which tty?
    struct stat sb;
    JniString filename(m_env, slavePtyName.get());
    if (stat(filename.str().c_str(), &sb) != 0) {
        throw unix_exception("stat(" + filename.str() + ", &sb) failed");
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
    std::deque<std::string> processNames;
    int count = byteCount / sizeof(kinfo_proc);
    kinfo_proc* kp = (kinfo_proc*) &buffer[0];
    for (int i = 0; i < count; ++i) {
        // FIXME: can we easily sort these into "ps -Helf" order?
        processNames.push_back(std::string(kp->kp_proc.p_comm) + "(" + toString(kp->kp_proc.p_pid) + ")");
        ++kp;
    }
    
    return newStringUtf8(join(", ", processNames));
#endif
}
