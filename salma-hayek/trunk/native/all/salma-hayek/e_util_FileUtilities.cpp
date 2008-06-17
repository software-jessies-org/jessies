#ifdef __CYGWIN__
// Fix jni_md.h:16: error: `__int64' does not name a type
#include <windows.h>
#endif

#include "e_util_FileUtilities.h"
#include "JniString.h"
#include "unix_exception.h"

#include <sys/types.h>
#include <sys/stat.h>

jboolean e_util_FileUtilities::nativeIsSymbolicLink(jstring javaFilename) {
    std::string filename(JniString(m_env, javaFilename));
    struct stat sb;
    int rc = lstat(filename.c_str(), &sb);
    if (rc != 0) {
        throw unix_exception("lstat(\"" + filename + "\") failed");
    }
    return S_ISLNK(sb.st_mode) ? JNI_TRUE : JNI_FALSE;
}
