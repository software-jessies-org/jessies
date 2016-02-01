#ifndef JNI_STRING_H_included
#define JNI_STRING_H_included

#include "PortableJni.h"

#include <stdexcept>
#include <string>

/**
 * Copies the characters from a jstring and makes them available.
 */
std::string JniString(JNIEnv* env, jstring instance) {
    const char* utf8Chars = env->GetStringUTFChars(instance, 0);
    if (utf8Chars == 0) {
        throw std::runtime_error("GetStringUTFChars returned 0");
    }
    std::string result(utf8Chars);
    env->ReleaseStringUTFChars(instance, utf8Chars);
    return result;
}

#endif
