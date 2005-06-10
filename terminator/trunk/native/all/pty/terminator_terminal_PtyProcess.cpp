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

#include <iostream>
#include <sstream>
#include <stdexcept>
#include <string>

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

static jfieldID getFieldID(JNIEnv* env, jobject object, const char* fieldName, const char* fieldSignature) {
    jclass objectClass = env->GetObjectClass(object);
    jfieldID result = env->GetFieldID(objectClass, fieldName, fieldSignature);
    if (result == 0) {
        throw std::runtime_error(std::string() + "couldn't find field " + fieldName + " (" + fieldSignature + ")");
    }
    return result;
}

// ---------------------------------------------------------------------------

template <typename T>
static void setField(JNIEnv* env, jobject object, const char* fieldName, const T& newValue);

template <>
static void setField(JNIEnv* env, jobject object, const char* fieldName, const int& newValue) {
    env->SetIntField(object, getFieldID(env, object, fieldName, "I"), newValue);
}

template <>
static void setField(JNIEnv* env, jobject object, const char* fieldName, const bool& newValue) {
    env->SetIntField(object, getFieldID(env, object, fieldName, "Z"), newValue ? JNI_TRUE : JNI_FALSE);
}

// ---------------------------------------------------------------------------

static int getIntField(JNIEnv* env, jobject object, const char* fieldName) {
    return env->GetIntField(object, getFieldID(env, object, fieldName, "I"));
}

// ---------------------------------------------------------------------------

static pid_t getPid(JNIEnv *env, jobject ptyProcess) {
    return (pid_t) getIntField(env, ptyProcess, "processId");
}

static void clientPanic(int fd, const char *message) {
    FILE* tunnel = fdopen(fd, "w");
    fprintf(tunnel, "Error from child: %s\n", message);
    exit(1);
}

static int runChild(char * const *cmd, PtyGenerator& ptyGenerator) {
    if (setsid() < 0) {
        fprintf(stderr, "Failed to setsid.\n");
        exit(1);
    }
    int childFd = ptyGenerator.openSlaveAndCloseMaster();
    if (childFd < 0) {
        fprintf(stderr, "Failed to open client's side of the pty.\n");
        exit(2);
    }
#if defined(TIOCSCTTY) && !defined(CIBAUD)
    /* 44BSD way to acquire controlling terminal */
    /* !CIBAUD to avoid doing this under SunOS */
    if (ioctl(childFd, TIOCSCTTY, (char *) 0) < 0) {
        clientPanic(childFd, "TIOCSCTTY error");
    }
#endif
    /* slave becomes stdin/stdout/stderr of child */
    if (dup2(childFd, STDIN_FILENO) != STDIN_FILENO) {
        clientPanic(childFd, "dup2 error to stdin");
    }
    if (dup2(childFd, STDOUT_FILENO) != STDOUT_FILENO) {
        clientPanic(childFd, "dup2 error to stdout");
    }
    if (dup2(childFd, STDERR_FILENO) != STDERR_FILENO) {
        clientPanic(childFd, "dup2 error to stderr");
    }
    if (childFd > STDERR_FILENO) {
        close(childFd);
    }
    putenv("TERM=terminator");
    
    /*
     * rxvt resets these signal handlers, and we'll do the same, because it magically
     * fixes the bug where ^c doesn't work if we're launched from KDE or Gnome's
     * launcher program.  I don't quite understand why - maybe bash reads the existing
     * SIGINT setting, and if it's set to something other than DFL it lets the parent process
     * take care of job control.
     */
    signal(SIGINT, SIG_DFL);
    signal(SIGQUIT, SIG_DFL);
    signal(SIGCHLD, SIG_DFL);
    
    if (execvp(cmd[0], cmd) < 0) {
        fprintf(stderr, "Error from child: Can't execute %s\n", cmd[0]);
        exit(1);
    }
    return(0);        /* child returns 0 just like fork() */
}

static pid_t doExecution(char * const *cmd, PtyGenerator& ptyGenerator) {
    pid_t pid = fork();
    if (pid < 0) {
        return -1;
    } else if (pid == 0) {
        return runChild(cmd, ptyGenerator);  // Should never return.
    } else {
        return pid;
    }
}

static void appendErrno(std::ostream& message) {
    message << ": (errno=" << errno;
    if (errno != 0) {
        message << " - " << errnoToString(errno);
    }
    message << ")";
}

static void throwJavaIOException(JNIEnv* env, const std::exception& ex) {
    std::ostringstream oss;
    oss << ex.what();
    appendErrno(oss);
    
    jclass exceptionClass = env->FindClass("java/io/IOException");
    if (exceptionClass) {
        env->ThrowNew(exceptionClass, oss.str().c_str());
    }
}

static void PtyProcess_startProcess(JNIEnv *env, jobject ptyProcess, jobjectArray command, jobject inDescriptor, jobject outDescriptor) {
    int cmdLength = env->GetArrayLength(command);
    char **cmd = (char **) calloc(cmdLength + 1, sizeof(char *));
    jstring *strings = (jstring *) calloc(cmdLength, sizeof(jstring));
    for (int i = 0; i < cmdLength; i++) {
        jobject obj = env->GetObjectArrayElement(command, i);
        strings[i] = (jstring) obj;
        cmd[i] = const_cast<char*>(env->GetStringUTFChars(strings[i], 0));
    }
    PtyGenerator ptyGenerator;
    if (ptyGenerator.openMaster() == false) {
        throw std::runtime_error("couldn't open master");
    }
    pid_t pid = doExecution(cmd, ptyGenerator);
    setField(env, ptyProcess, "processId", pid);
    for (int i = 0; i < cmdLength; i++) {
        env->ReleaseStringUTFChars(strings[i], cmd[i]);
    }
    free(strings);
    free(cmd);
    int masterFd = ptyGenerator.getMasterFd();
    setField(env, inDescriptor, "fd", masterFd);
    setField(env, outDescriptor, "fd", masterFd);
    setField(env, ptyProcess, "fd", masterFd);
}

extern "C" void Java_terminator_terminal_PtyProcess_startProcess(JNIEnv *env, jobject ptyProcess, jobjectArray command, jobject inDescriptor, jobject outDescriptor) {
    try {
        PtyProcess_startProcess(env, ptyProcess, command, inDescriptor, outDescriptor);
    } catch (const std::exception& ex) {
        throwJavaIOException(env, ex);
    }
}

static void PtyProcess_sendResizeNotification(JNIEnv *env, jobject ptyProcess, jobject sizeInChars, jobject sizeInPixels) {
    struct winsize size;
    size.ws_col = getIntField(env, sizeInChars, "width");
    size.ws_row = getIntField(env, sizeInChars, "height");
    size.ws_xpixel = getIntField(env, sizeInPixels, "width");
    size.ws_ypixel = getIntField(env, sizeInPixels, "height");
    int fd = getIntField(env, ptyProcess, "fd");
    if (ioctl(fd, TIOCSWINSZ, (char *) &size) < 0) {
        throw std::runtime_error("ioctl(" + toString(fd) + ", TIOCSWINSZ, &size) failed");
    }
}

extern "C" void Java_terminator_terminal_PtyProcess_sendResizeNotification(JNIEnv *env, jobject ptyProcess, jobject sizeInChars, jobject sizeInPixels) {
    try {
        PtyProcess_sendResizeNotification(env, ptyProcess, sizeInChars, sizeInPixels);
    } catch (const std::exception& ex) {
        throwJavaIOException(env, ex);
    }
}

static void PtyProcess_destroy(JNIEnv *env, jobject ptyProcess) {
    pid_t pid = getPid(env, ptyProcess);
    int status = killpg(pid, SIGHUP);
    if (status < 0) {
        throw std::runtime_error("killpg(" + toString(pid) + ", SIGHUP) failed");
    }
}

extern "C" void Java_terminator_terminal_PtyProcess_destroy(JNIEnv *env, jobject ptyProcess) {
    try {
        PtyProcess_destroy(env, ptyProcess);
    } catch (const std::exception& ex) {
        throwJavaIOException(env, ex);
    }
}

static void PtyProcess_waitFor(JNIEnv *env, jobject ptyProcess) {
    pid_t pid = getPid(env, ptyProcess);
    int status;
    pid_t result = waitpid(pid, &status, 0);
    if (result < 0) {
        throw std::runtime_error("waitpid(" + toString(pid) + ", &status, 0) failed");
    }
    
    int exitValue = WEXITSTATUS(status);
    setField(env, ptyProcess, "hasTerminated", true);
    setField(env, ptyProcess, "exitValue", exitValue);
}

extern "C" void Java_terminator_terminal_PtyProcess_waitFor(JNIEnv *env, jobject ptyProcess) {
    try {
        PtyProcess_waitFor(env, ptyProcess);
    } catch (std::exception& ex) {
        throwJavaIOException(env, ex);
    }
}

#endif
