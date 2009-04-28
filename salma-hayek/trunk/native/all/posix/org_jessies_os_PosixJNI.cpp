#ifdef __CYGWIN__
// Fix jni_md.h:16: error: `__int64' does not name a type
#include <windows.h>
#endif

#include "org_jessies_os_PosixJNI.h"
#include "JniString.h"
#include "unix_exception.h"

#include <sys/stat.h>
#include <unistd.h>

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

static jint translateResult(int result) {
    return result == -1 ? -errno : 0;
}

jint org_jessies_os_PosixJNI::access(jstring javaPath, jint accessMode) {
    return translateResult(::access(JniString(m_env, javaPath).c_str(), accessMode));
}

jint org_jessies_os_PosixJNI::chown(jstring javaPath, jint uid, jint gid) {
    return translateResult(::chown(JniString(m_env, javaPath).c_str(), uid, gid));
}

jint org_jessies_os_PosixJNI::chmod(jstring javaPath, jint mode) {
    return translateResult(::chmod(JniString(m_env, javaPath).c_str(), mode));
}

static jint translateStat(JNIEnv* env, jobject javaStat, const struct stat& sb) {
    jclass statClass = env->FindClass("org/jessies/os/Stat");
    if (statClass == NULL) {
        return NULL; // Exception already thrown.
    }
    jmethodID setter = env->GetMethodID(statClass, "set", "(JJIJIIJJJJJJJ)V");
    if (setter == NULL) {
        return NULL; // Exception already thrown.
    }
    env->CallVoidMethod(javaStat, setter, sb.st_dev, sb.st_ino, sb.st_mode, sb.st_nlink, sb.st_uid, sb.st_gid, sb.st_rdev, sb.st_size, sb.st_atime, sb.st_mtime, sb.st_ctime, sb.st_blksize, sb.st_blocks);
    return 0;
}

jint org_jessies_os_PosixJNI::lstat(jstring javaPath, jobject javaStat) {
    struct stat sb;
    if (::lstat(JniString(m_env, javaPath).c_str(), &sb) != 0) {
        return -errno;
    }
    return translateStat(m_env, javaStat, sb);
}

jint org_jessies_os_PosixJNI::stat(jstring javaPath, jobject javaStat) {
    struct stat sb;
    if (::stat(JniString(m_env, javaPath).c_str(), &sb) != 0) {
        return -errno;
    }
    return translateStat(m_env, javaStat, sb);
}
