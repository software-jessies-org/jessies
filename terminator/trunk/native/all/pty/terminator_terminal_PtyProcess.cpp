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

#include "errnoToString.h"
#include "PtyGenerator.h"

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

// ---------------------------------------------------------------------------

// Based on code by Kevlin Henney, shown in "Exceptional C++ Style".
template <typename T>
std::string toString(const T& value) {
    std::stringstream interpreter;
    std::string result;
    if (!(interpreter << value) || !(interpreter >> result) || !(interpreter >> std::ws).eof()) {
        throw std::runtime_error("bad lexical cast");
    }
    return result;
}

// ---------------------------------------------------------------------------

struct Arguments : std::vector<std::string> {
    Arguments(JNIEnv* env, jobjectArray command) {
        int arrayLength = env->GetArrayLength(command);
        for (int i = 0; i != arrayLength; ++i) {
            jstring javaString = (jstring) env->GetObjectArrayElement(command, i);
            const char* utfChars = env->GetStringUTFChars(javaString, 0);
            push_back(utfChars);
            env->ReleaseStringUTFChars(javaString, utfChars);
        }        
    }
};

struct Argv : std::vector<char*> {
    // Non-const because execvp is anti-social about const.
    Argv(Arguments& arguments) {
        for (Arguments::iterator it = arguments.begin(); it != arguments.end(); ++it) {
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
    
    Arguments arguments(m_env, command);
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
    if (ioctl(fd.get(), TIOCSWINSZ, (char *) &size) < 0) {
        throw std::runtime_error("ioctl(" + toString(fd.get()) + ", TIOCSWINSZ, &size) failed" + errnoToString());
    }
}

void terminator_terminal_PtyProcess::destroy() {
    pid_t pid = processId.get();
    int status = killpg(pid, SIGHUP);
    if (status < 0) {
        throw std::runtime_error("killpg(" + toString(pid) + ", SIGHUP) failed" + errnoToString());
    }
}

void terminator_terminal_PtyProcess::waitFor() {
    pid_t pid = processId.get();
    int status;
    pid_t result = waitpid(pid, &status, 0);
    if (result < 0) {
        throw std::runtime_error("waitpid(" + toString(pid) + ", &status, 0) failed" + errnoToString());
    }
    
    exitValue = WEXITSTATUS(status);
    hasTerminated = true;
}

#endif
