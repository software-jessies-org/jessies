#ifdef __CYGWIN__
// Fix jni_md.h:16: error: `__int64' does not name a type
#include <windows.h>
#endif

#include "org_jessies_os_PosixJNI.h"
#include "JniString.h"
#include "unix_exception.h"

#include <sys/stat.h>
#include <signal.h>
#include <unistd.h>

jint org_jessies_os_PosixJNI::get_1SEEK_1CUR() { return SEEK_CUR; }
jint org_jessies_os_PosixJNI::get_1SEEK_1END() { return SEEK_END; }
jint org_jessies_os_PosixJNI::get_1SEEK_1SET() { return SEEK_SET; }

jint org_jessies_os_PosixJNI::get_1R_1OK() { return R_OK; }
jint org_jessies_os_PosixJNI::get_1W_1OK() { return W_OK; }
jint org_jessies_os_PosixJNI::get_1X_1OK() { return X_OK; }
jint org_jessies_os_PosixJNI::get_1F_1OK() { return F_OK; }

jint org_jessies_os_PosixJNI::get_1S_1IFMT() { return S_IFMT; }
jint org_jessies_os_PosixJNI::get_1S_1IFBLK() { return S_IFBLK; }
jint org_jessies_os_PosixJNI::get_1S_1IFCHR() { return S_IFCHR; }
jint org_jessies_os_PosixJNI::get_1S_1IFIFO() { return S_IFIFO; }
jint org_jessies_os_PosixJNI::get_1S_1IFREG() { return S_IFREG; }
jint org_jessies_os_PosixJNI::get_1S_1IFDIR() { return S_IFDIR; }
jint org_jessies_os_PosixJNI::get_1S_1IFLNK() { return S_IFLNK; }
jint org_jessies_os_PosixJNI::get_1S_1IFSOCK() { return S_IFSOCK; }

jint org_jessies_os_PosixJNI::get_1S_1ISUID() { return S_ISUID; }
jint org_jessies_os_PosixJNI::get_1S_1ISGID() { return S_ISGID; }

jint org_jessies_os_PosixJNI::get_1SIGABRT() { return SIGABRT; }
jint org_jessies_os_PosixJNI::get_1SIGALRM() { return SIGALRM; }
jint org_jessies_os_PosixJNI::get_1SIGBUS() { return SIGBUS; }
jint org_jessies_os_PosixJNI::get_1SIGCHLD() { return SIGCHLD; }
jint org_jessies_os_PosixJNI::get_1SIGCONT() { return SIGCONT; }
jint org_jessies_os_PosixJNI::get_1SIGFPE() { return SIGFPE; }
jint org_jessies_os_PosixJNI::get_1SIGHUP() { return SIGHUP; }
jint org_jessies_os_PosixJNI::get_1SIGILL() { return SIGILL; }
jint org_jessies_os_PosixJNI::get_1SIGINT() { return SIGINT; }
jint org_jessies_os_PosixJNI::get_1SIGKILL() { return SIGKILL; }
jint org_jessies_os_PosixJNI::get_1SIGPIPE() { return SIGPIPE; }
jint org_jessies_os_PosixJNI::get_1SIGQUIT() { return SIGQUIT; }
jint org_jessies_os_PosixJNI::get_1SIGSEGV() { return SIGSEGV; }
jint org_jessies_os_PosixJNI::get_1SIGSTOP() { return SIGSTOP; }
jint org_jessies_os_PosixJNI::get_1SIGTERM() { return SIGTERM; }
jint org_jessies_os_PosixJNI::get_1SIGTSTP() { return SIGTSTP; }
jint org_jessies_os_PosixJNI::get_1SIGTTIN() { return SIGTTIN; }
jint org_jessies_os_PosixJNI::get_1SIGTTOU() { return SIGTTOU; }
jint org_jessies_os_PosixJNI::get_1SIGUSR1() { return SIGUSR1; }
jint org_jessies_os_PosixJNI::get_1SIGUSR2() { return SIGUSR2; }
jint org_jessies_os_PosixJNI::get_1SIGPROF() { return SIGPROF; }
jint org_jessies_os_PosixJNI::get_1SIGSYS() { return SIGSYS; }
jint org_jessies_os_PosixJNI::get_1SIGTRAP() { return SIGTRAP; }
jint org_jessies_os_PosixJNI::get_1SIGURG() { return SIGURG; }
jint org_jessies_os_PosixJNI::get_1SIGXCPU() { return SIGXCPU; }
jint org_jessies_os_PosixJNI::get_1SIGXFSZ() { return SIGXFSZ; }

static jint translateResult(int result) {
    return result == -1 ? -errno : 0;
}

jint org_jessies_os_PosixJNI::access(jstring javaPath, jint accessMode) {
    return translateResult(::access(JniString(m_env, javaPath).c_str(), accessMode));
}

jint org_jessies_os_PosixJNI::chmod(jstring javaPath, jint mode) {
    return translateResult(::chmod(JniString(m_env, javaPath).c_str(), mode));
}

jint org_jessies_os_PosixJNI::chown(jstring javaPath, jint uid, jint gid) {
    return translateResult(::chown(JniString(m_env, javaPath).c_str(), uid, gid));
}

jint org_jessies_os_PosixJNI::close(jint fd) {
    return translateResult(::close(fd));
}

jint org_jessies_os_PosixJNI::dup(jint oldFd) {
    return translateResult(::dup(oldFd));
}

jint org_jessies_os_PosixJNI::dup2(jint oldFd, jint newFd) {
    return translateResult(::dup2(oldFd, newFd));
}

jint org_jessies_os_PosixJNI::getpid() {
    return ::getpid();
}

jint org_jessies_os_PosixJNI::getppid() {
    return ::getppid();
}

static void translateStat(JNIEnv* env, jobject javaStat, const struct stat& sb) {
    jclass statClass = env->FindClass("org/jessies/os/Stat");
    jmethodID setter = env->GetMethodID(statClass, "set", "(JJIJIIJJJJJJJ)V");
    env->CallVoidMethod(javaStat, setter, jlong(sb.st_dev), jlong(sb.st_ino), jint(sb.st_mode), jlong(sb.st_nlink), jint(sb.st_uid), jint(sb.st_gid), jlong(sb.st_rdev), jlong(sb.st_size), jlong(sb.st_atime), jlong(sb.st_mtime), jlong(sb.st_ctime), jlong(sb.st_blksize), jlong(sb.st_blocks));
}

jint org_jessies_os_PosixJNI::lstat(jstring javaPath, jobject javaStat) {
    struct stat sb;
    if (::lstat(JniString(m_env, javaPath).c_str(), &sb) != 0) {
        return -errno;
    }
    translateStat(m_env, javaStat, sb);
    return 0;
}

jint org_jessies_os_PosixJNI::stat(jstring javaPath, jobject javaStat) {
    struct stat sb;
    if (::stat(JniString(m_env, javaPath).c_str(), &sb) != 0) {
        return -errno;
    }
    translateStat(m_env, javaStat, sb);
    return 0;
}
