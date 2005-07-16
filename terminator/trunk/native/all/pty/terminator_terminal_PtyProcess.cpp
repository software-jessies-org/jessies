#ifdef __CYGWIN__
typedef long long __int64;
#endif

#include "terminator_terminal_PtyProcess.h"

#ifdef _WIN32

extern "C" void Java_terminator_terminal_PtyProcess_startProcess(JNIEnv *, jobject, jobjectArray, jobject, jobject) {
}
extern "C" void Java_terminator_terminal_PtyProcess_sendResizeNotification(JNIEnv *, jobject, jobject, jobject) {
}
extern "C" void Java_terminator_terminal_PtyProcess_destroy(JNIEnv *, jobject) {
}
extern "C" void Java_terminator_terminal_PtyProcess_waitFor(JNIEnv *, jobject) {
}

#else

#include <JniString.h>

#include "PtyGenerator.h"
#include "toString.h"
#include "unix_exception.h"

#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <termios.h>

#include <sys/types.h>
#include <sys/wait.h>

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

void terminator_terminal_PtyProcess::startProcess(jobjectArray command, jobject inDescriptor, jobject outDescriptor) {
    PtyGenerator ptyGenerator;
    int masterFd = ptyGenerator.openMaster();
    
    JavaStringArrayToStringArray arguments(m_env, command);
    Argv argv(arguments);
    processId = ptyGenerator.forkAndExec(&argv[0]);
    
    fd = masterFd;
    JniField<jint>(m_env, inDescriptor, "fd", "I") = masterFd;
    JniField<jint>(m_env, outDescriptor, "fd", "I") = masterFd;
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

void terminator_terminal_PtyProcess::waitFor() {
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
    }
}

#endif
