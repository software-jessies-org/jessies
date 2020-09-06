#ifdef __CYGWIN__
// Fix jni_md.h:16: error: `__int64' does not name a type
#include <windows.h>
#endif

#include "JniString.h"
#include "org_jessies_os_PosixJNI.h"
#include "unix_exception.h"

#include <errno.h>
#include <fcntl.h>
#include <pwd.h>
#include <signal.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <vector>

jint org_jessies_os_PosixJNI::get_1EXIT_1FAILURE() {
  return EXIT_FAILURE;
}

jint org_jessies_os_PosixJNI::get_1SEEK_1CUR() {
  return SEEK_CUR;
}
jint org_jessies_os_PosixJNI::get_1SEEK_1END() {
  return SEEK_END;
}
jint org_jessies_os_PosixJNI::get_1SEEK_1SET() {
  return SEEK_SET;
}

jint org_jessies_os_PosixJNI::get_1R_1OK() {
  return R_OK;
}
jint org_jessies_os_PosixJNI::get_1W_1OK() {
  return W_OK;
}
jint org_jessies_os_PosixJNI::get_1X_1OK() {
  return X_OK;
}
jint org_jessies_os_PosixJNI::get_1F_1OK() {
  return F_OK;
}

jint org_jessies_os_PosixJNI::get_1S_1IFMT() {
  return S_IFMT;
}
jint org_jessies_os_PosixJNI::get_1S_1IFBLK() {
  return S_IFBLK;
}
jint org_jessies_os_PosixJNI::get_1S_1IFCHR() {
  return S_IFCHR;
}
jint org_jessies_os_PosixJNI::get_1S_1IFIFO() {
  return S_IFIFO;
}
jint org_jessies_os_PosixJNI::get_1S_1IFREG() {
  return S_IFREG;
}
jint org_jessies_os_PosixJNI::get_1S_1IFDIR() {
  return S_IFDIR;
}
jint org_jessies_os_PosixJNI::get_1S_1IFLNK() {
  return S_IFLNK;
}
jint org_jessies_os_PosixJNI::get_1S_1IFSOCK() {
  return S_IFSOCK;
}

jint org_jessies_os_PosixJNI::get_1S_1ISUID() {
  return S_ISUID;
}
jint org_jessies_os_PosixJNI::get_1S_1ISGID() {
  return S_ISGID;
}

jint org_jessies_os_PosixJNI::get_1SIGABRT() {
  return SIGABRT;
}
jint org_jessies_os_PosixJNI::get_1SIGALRM() {
  return SIGALRM;
}
jint org_jessies_os_PosixJNI::get_1SIGBUS() {
  return SIGBUS;
}
jint org_jessies_os_PosixJNI::get_1SIGCHLD() {
  return SIGCHLD;
}
jint org_jessies_os_PosixJNI::get_1SIGCONT() {
  return SIGCONT;
}
jint org_jessies_os_PosixJNI::get_1SIGFPE() {
  return SIGFPE;
}
jint org_jessies_os_PosixJNI::get_1SIGHUP() {
  return SIGHUP;
}
jint org_jessies_os_PosixJNI::get_1SIGILL() {
  return SIGILL;
}
jint org_jessies_os_PosixJNI::get_1SIGINT() {
  return SIGINT;
}
jint org_jessies_os_PosixJNI::get_1SIGKILL() {
  return SIGKILL;
}
jint org_jessies_os_PosixJNI::get_1SIGPIPE() {
  return SIGPIPE;
}
jint org_jessies_os_PosixJNI::get_1SIGQUIT() {
  return SIGQUIT;
}
jint org_jessies_os_PosixJNI::get_1SIGSEGV() {
  return SIGSEGV;
}
jint org_jessies_os_PosixJNI::get_1SIGSTOP() {
  return SIGSTOP;
}
jint org_jessies_os_PosixJNI::get_1SIGTERM() {
  return SIGTERM;
}
jint org_jessies_os_PosixJNI::get_1SIGTSTP() {
  return SIGTSTP;
}
jint org_jessies_os_PosixJNI::get_1SIGTTIN() {
  return SIGTTIN;
}
jint org_jessies_os_PosixJNI::get_1SIGTTOU() {
  return SIGTTOU;
}
jint org_jessies_os_PosixJNI::get_1SIGUSR1() {
  return SIGUSR1;
}
jint org_jessies_os_PosixJNI::get_1SIGUSR2() {
  return SIGUSR2;
}
jint org_jessies_os_PosixJNI::get_1SIGPROF() {
  return SIGPROF;
}
jint org_jessies_os_PosixJNI::get_1SIGSYS() {
  return SIGSYS;
}
jint org_jessies_os_PosixJNI::get_1SIGTRAP() {
  return SIGTRAP;
}
jint org_jessies_os_PosixJNI::get_1SIGURG() {
  return SIGURG;
}
jint org_jessies_os_PosixJNI::get_1SIGXCPU() {
  return SIGXCPU;
}
jint org_jessies_os_PosixJNI::get_1SIGXFSZ() {
  return SIGXFSZ;
}

jint org_jessies_os_PosixJNI::get_1O_1CREAT() {
  return O_CREAT;
}
jint org_jessies_os_PosixJNI::get_1O_1EXCL() {
  return O_EXCL;
}
jint org_jessies_os_PosixJNI::get_1O_1NOCTTY() {
  return O_NOCTTY;
}
jint org_jessies_os_PosixJNI::get_1O_1TRUNC() {
  return O_TRUNC;
}

jint org_jessies_os_PosixJNI::get_1O_1APPEND() {
  return O_APPEND;
}

jint org_jessies_os_PosixJNI::get_1O_1RDONLY() {
  return O_RDONLY;
}
jint org_jessies_os_PosixJNI::get_1O_1RDWR() {
  return O_RDWR;
}
jint org_jessies_os_PosixJNI::get_1O_1WRONLY() {
  return O_WRONLY;
}

#ifdef __CYGWIN__
// Not supported on Cygwin 1.5.25.
// Not supported on Cygwin 1.7.0.
// 8 is the value on Linux and Cygwin generally follows Linux.
#ifndef WCONTINUED
#define WCONTINUED 8
#endif
#endif
jint org_jessies_os_PosixJNI::get_1WCONTINUED() {
  return WCONTINUED;
}
jint org_jessies_os_PosixJNI::get_1WNOHANG() {
  return WNOHANG;
}
jint org_jessies_os_PosixJNI::get_1WUNTRACED() {
  return WUNTRACED;
}

jint org_jessies_os_PosixJNI::get_1E2BIG() {
  return E2BIG;
}
jint org_jessies_os_PosixJNI::get_1EACCES() {
  return EACCES;
}
jint org_jessies_os_PosixJNI::get_1EADDRINUSE() {
  return EADDRINUSE;
}
jint org_jessies_os_PosixJNI::get_1EADDRNOTAVAIL() {
  return EADDRNOTAVAIL;
}
jint org_jessies_os_PosixJNI::get_1EAFNOSUPPORT() {
  return EAFNOSUPPORT;
}
jint org_jessies_os_PosixJNI::get_1EAGAIN() {
  return EAGAIN;
}
jint org_jessies_os_PosixJNI::get_1EALREADY() {
  return EALREADY;
}
jint org_jessies_os_PosixJNI::get_1EBADF() {
  return EBADF;
}
jint org_jessies_os_PosixJNI::get_1EBADMSG() {
  return EBADMSG;
}
jint org_jessies_os_PosixJNI::get_1EBUSY() {
  return EBUSY;
}
#ifdef __CYGWIN__
// Not supported on Cygwin 1.5.25.
// 140 is the value on Cygwin 1.7.0.
#ifndef ECANCELED
#define ECANCELED 140
#endif
#endif
jint org_jessies_os_PosixJNI::get_1ECANCELED() {
  return ECANCELED;
}
jint org_jessies_os_PosixJNI::get_1ECHILD() {
  return ECHILD;
}
jint org_jessies_os_PosixJNI::get_1ECONNABORTED() {
  return ECONNABORTED;
}
jint org_jessies_os_PosixJNI::get_1ECONNREFUSED() {
  return ECONNREFUSED;
}
jint org_jessies_os_PosixJNI::get_1ECONNRESET() {
  return ECONNRESET;
}
jint org_jessies_os_PosixJNI::get_1EDEADLK() {
  return EDEADLK;
}
jint org_jessies_os_PosixJNI::get_1EDESTADDRREQ() {
  return EDESTADDRREQ;
}
jint org_jessies_os_PosixJNI::get_1EDOM() {
  return EDOM;
}
jint org_jessies_os_PosixJNI::get_1EDQUOT() {
  return EDQUOT;
}
jint org_jessies_os_PosixJNI::get_1EEXIST() {
  return EEXIST;
}
jint org_jessies_os_PosixJNI::get_1EFAULT() {
  return EFAULT;
}
jint org_jessies_os_PosixJNI::get_1EFBIG() {
  return EFBIG;
}
jint org_jessies_os_PosixJNI::get_1EHOSTUNREACH() {
  return EHOSTUNREACH;
}
jint org_jessies_os_PosixJNI::get_1EIDRM() {
  return EIDRM;
}
jint org_jessies_os_PosixJNI::get_1EILSEQ() {
  return EILSEQ;
}
jint org_jessies_os_PosixJNI::get_1EINPROGRESS() {
  return EINPROGRESS;
}
jint org_jessies_os_PosixJNI::get_1EINTR() {
  return EINTR;
}
jint org_jessies_os_PosixJNI::get_1EINVAL() {
  return EINVAL;
}
jint org_jessies_os_PosixJNI::get_1EIO() {
  return EIO;
}
jint org_jessies_os_PosixJNI::get_1EISCONN() {
  return EISCONN;
}
jint org_jessies_os_PosixJNI::get_1EISDIR() {
  return EISDIR;
}
jint org_jessies_os_PosixJNI::get_1ELOOP() {
  return ELOOP;
}
jint org_jessies_os_PosixJNI::get_1EMFILE() {
  return EMFILE;
}
jint org_jessies_os_PosixJNI::get_1EMLINK() {
  return EMLINK;
}
jint org_jessies_os_PosixJNI::get_1EMSGSIZE() {
  return EMSGSIZE;
}
jint org_jessies_os_PosixJNI::get_1EMULTIHOP() {
  return EMULTIHOP;
}
jint org_jessies_os_PosixJNI::get_1ENAMETOOLONG() {
  return ENAMETOOLONG;
}
jint org_jessies_os_PosixJNI::get_1ENETDOWN() {
  return ENETDOWN;
}
jint org_jessies_os_PosixJNI::get_1ENETRESET() {
  return ENETRESET;
}
jint org_jessies_os_PosixJNI::get_1ENETUNREACH() {
  return ENETUNREACH;
}
jint org_jessies_os_PosixJNI::get_1ENFILE() {
  return ENFILE;
}
jint org_jessies_os_PosixJNI::get_1ENOBUFS() {
  return ENOBUFS;
}
jint org_jessies_os_PosixJNI::get_1ENODEV() {
  return ENODEV;
}
jint org_jessies_os_PosixJNI::get_1ENOENT() {
  return ENOENT;
}
jint org_jessies_os_PosixJNI::get_1ENOEXEC() {
  return ENOEXEC;
}
jint org_jessies_os_PosixJNI::get_1ENOLCK() {
  return ENOLCK;
}
jint org_jessies_os_PosixJNI::get_1ENOLINK() {
  return ENOLINK;
}
jint org_jessies_os_PosixJNI::get_1ENOMEM() {
  return ENOMEM;
}
jint org_jessies_os_PosixJNI::get_1ENOMSG() {
  return ENOMSG;
}
jint org_jessies_os_PosixJNI::get_1ENOPROTOOPT() {
  return ENOPROTOOPT;
}
jint org_jessies_os_PosixJNI::get_1ENOSPC() {
  return ENOSPC;
}
jint org_jessies_os_PosixJNI::get_1ENOSYS() {
  return ENOSYS;
}
jint org_jessies_os_PosixJNI::get_1ENOTCONN() {
  return ENOTCONN;
}
jint org_jessies_os_PosixJNI::get_1ENOTDIR() {
  return ENOTDIR;
}
jint org_jessies_os_PosixJNI::get_1ENOTEMPTY() {
  return ENOTEMPTY;
}
jint org_jessies_os_PosixJNI::get_1ENOTSOCK() {
  return ENOTSOCK;
}
jint org_jessies_os_PosixJNI::get_1ENOTSUP() {
  return ENOTSUP;
}
jint org_jessies_os_PosixJNI::get_1ENOTTY() {
  return ENOTTY;
}
jint org_jessies_os_PosixJNI::get_1ENXIO() {
  return ENXIO;
}
jint org_jessies_os_PosixJNI::get_1EOPNOTSUPP() {
  return EOPNOTSUPP;
}
jint org_jessies_os_PosixJNI::get_1EOVERFLOW() {
  return EOVERFLOW;
}
jint org_jessies_os_PosixJNI::get_1EPERM() {
  return EPERM;
}
jint org_jessies_os_PosixJNI::get_1EPIPE() {
  return EPIPE;
}
jint org_jessies_os_PosixJNI::get_1EPROTO() {
  return EPROTO;
}
jint org_jessies_os_PosixJNI::get_1EPROTONOSUPPORT() {
  return EPROTONOSUPPORT;
}
jint org_jessies_os_PosixJNI::get_1EPROTOTYPE() {
  return EPROTOTYPE;
}
jint org_jessies_os_PosixJNI::get_1ERANGE() {
  return ERANGE;
}
jint org_jessies_os_PosixJNI::get_1EROFS() {
  return EROFS;
}
jint org_jessies_os_PosixJNI::get_1ESPIPE() {
  return ESPIPE;
}
jint org_jessies_os_PosixJNI::get_1ESRCH() {
  return ESRCH;
}
jint org_jessies_os_PosixJNI::get_1ESTALE() {
  return ESTALE;
}
jint org_jessies_os_PosixJNI::get_1ETIMEDOUT() {
  return ETIMEDOUT;
}
jint org_jessies_os_PosixJNI::get_1ETXTBSY() {
  return ETXTBSY;
}
jint org_jessies_os_PosixJNI::get_1EWOULDBLOCK() {
  return EWOULDBLOCK;
}
jint org_jessies_os_PosixJNI::get_1EXDEV() {
  return EXDEV;
}

static jint zeroOrMinusErrno(int result) {
  return result == -1 ? -errno : 0;
}

static jint resultOrMinusErrno(int result) {
  return result == -1 ? -errno : result;
}

jint org_jessies_os_PosixJNI::close(jint fd) {
  return zeroOrMinusErrno(::close(fd));
}

jint org_jessies_os_PosixJNI::getpid() {
  return ::getpid();
}

jint org_jessies_os_PosixJNI::kill(jint pid, jint signal) {
  return zeroOrMinusErrno(::kill(pid, signal));
}

jint org_jessies_os_PosixJNI::killpg(jint group, jint signal) {
  return zeroOrMinusErrno(::killpg(group, signal));
}

jint org_jessies_os_PosixJNI::tcgetpgrp(jint fd) {
  return resultOrMinusErrno(::tcgetpgrp(fd));
}

static jobject translatePasswd(JNIEnv* env, const passwd& pw) {
  jstring name(env->NewStringUTF(pw.pw_name));
  jint uid(pw.pw_uid);
  jint gid(pw.pw_gid);
  jstring dir(env->NewStringUTF(pw.pw_dir));
  jstring shell(env->NewStringUTF(pw.pw_shell));
  jclass passwdClass = env->FindClass("org/jessies/os/Passwd");
  jmethodID constructor = env->GetMethodID(
      passwdClass, "<init>",
      "(Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;)V");
  return env->NewObject(passwdClass, constructor, name, uid, gid, dir, shell);
}

jobject org_jessies_os_PosixJNI::getpwnam(jstring name) {
  passwd pw;
  passwd* pwp;
  char buf[1024];
  if (getpwnam_r(JniString(m_env, name).c_str(), &pw, buf, sizeof(buf), &pwp) !=
          0 ||
      pwp == NULL) {
    return NULL;
  }
  return translatePasswd(m_env, *pwp);
}

jobject org_jessies_os_PosixJNI::getpwuid(jint uid) {
  passwd pw;
  passwd* pwp;
  char buf[1024];
  if (getpwuid_r(uid, &pw, buf, sizeof(buf), &pwp) != 0 || pwp == NULL) {
    return NULL;
  }
  return translatePasswd(m_env, *pwp);
}

jstring org_jessies_os_PosixJNI::strerror(jint error) {
  return m_env->NewStringUTF(unix_exception::errnoToString(error).c_str());
}

jint org_jessies_os_PosixJNI::waitpid(jint pid,
                                      jobject javaWaitStatus,
                                      jint flags) {
  int status = 0;
  const pid_t result = ::waitpid(pid, &status, flags);
  if (result == -1) {
    return -errno;
  }
  JniField<jint, false> statusField(m_env, javaWaitStatus, "status", "I");
  statusField = status;
  return result;
}

jboolean org_jessies_os_PosixJNI::WCoreDump(jint status) {
#ifdef WCOREDUMP
  return WCOREDUMP(status) ? JNI_TRUE : JNI_FALSE;
#else
  return JNI_FALSE;
#endif
}
jint org_jessies_os_PosixJNI::WExitStatus(jint status) {
  return WEXITSTATUS(status);
}
#ifdef __CYGWIN__
// Not supported on Cygwin 1.5.25.
// Not supported on Cygwin 1.7.0.
#ifndef WIFCONTINUED
#define WIFCONTINUED(status) ((void)(status), false)
#endif
#endif
jboolean org_jessies_os_PosixJNI::WIfContinued(jint status) {
  return WIFCONTINUED(status);
}
jboolean org_jessies_os_PosixJNI::WIfExited(jint status) {
  return WIFEXITED(status);
}
jboolean org_jessies_os_PosixJNI::WIfSignaled(jint status) {
  return WIFSIGNALED(status);
}
jboolean org_jessies_os_PosixJNI::WIfStopped(jint status) {
  return WIFSTOPPED(status);
}
jint org_jessies_os_PosixJNI::WStopSig(jint status) {
  return WSTOPSIG(status);
}
jint org_jessies_os_PosixJNI::WTermSig(jint status) {
  return WTERMSIG(status);
}

static jint doWrite(JNIEnv* env,
                    jint fd,
                    jbyteArray buffer,
                    jint bufferOffset,
                    jint byteCount,
                    jlong fileOffset,
                    bool isPWrite) {
  // On Cygwin, on 2006-09-28, attempting a zero-byte write caused the JVM to
  // crash with an EXCEPTION_ACCESS_VIOLATION in a "cygwin1.dll" stack frame. So
  // let's make sure we never do that.
  if (byteCount == 0) {
    return 0;
  }

  // Allocate C heap to contain the bytes, and copy them in.
  // GetByteArrayRegion requires less code than
  // GetByteArrayElements/ReleaseByteArrayElements, and measurement shows that
  // the construction of the std::vector is more expensive anyway.
  std::vector<jbyte> nativeBuffer(byteCount);
  env->GetByteArrayRegion(buffer, bufferOffset, byteCount, &nativeBuffer[0]);
  if (env->ExceptionCheck()) {
    return -1;  // It doesn't matter what we return, because a Java exception
                // will be thrown.
  }

  return resultOrMinusErrno(
      isPWrite ? ::pwrite(fd, &nativeBuffer[0], byteCount, fileOffset)
               : ::write(fd, &nativeBuffer[0], byteCount));
}

jint org_jessies_os_PosixJNI::write(jint fd,
                                    jbyteArray buffer,
                                    jint bufferOffset,
                                    jint byteCount) {
  return doWrite(m_env, fd, buffer, bufferOffset, byteCount, 0, false);
}

static jint doRead(JNIEnv* env,
                   jint fd,
                   jbyteArray buffer,
                   jint bufferOffset,
                   jint byteCount,
                   jlong fileOffset,
                   bool isPRead) {
  // Zero byte reads seem to work even on Cygwin, but let's eliminate the
  // opportunity for crashing in this corner case.
  if (byteCount == 0) {
    return 0;
  }

  // Allocate C heap to contain the bytes, and read them in.
  std::vector<jbyte> nativeBuffer(byteCount);
  ssize_t bytesTransferred =
      isPRead ? ::pread(fd, &nativeBuffer[0], byteCount, fileOffset)
              : ::read(fd, &nativeBuffer[0], byteCount);
  if (bytesTransferred > 0) {
    // Copy any bytes transferred back in to the Java heap.
    env->SetByteArrayRegion(buffer, bufferOffset, bytesTransferred,
                            &nativeBuffer[0]);
  }

  return resultOrMinusErrno(bytesTransferred);
}

jint org_jessies_os_PosixJNI::read(jint fd,
                                   jbyteArray buffer,
                                   jint bufferOffset,
                                   jint byteCount) {
  return doRead(m_env, fd, buffer, bufferOffset, byteCount, 0, false);
}
