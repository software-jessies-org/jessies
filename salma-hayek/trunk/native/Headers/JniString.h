#ifndef JNI_STRING_H_included
#define JNI_STRING_H_included

#include <jni.h>
#include <string>

/**
 * Copies the characters from a jstring and makes them available in the
 * form of either a C or C++ string.
 */
class JniString {
    std::string m_utf8;
    
public:
    JniString(JNIEnv* env, jstring instance) {
        const char* utf8Chars = env->GetStringUTFChars(instance, 0);
        m_utf8.assign(utf8Chars);
        env->ReleaseStringUTFChars(instance, utf8Chars);
    }
    
    const char* c_str() const {
        return m_utf8.c_str();
    }
    
    const std::string str() const {
        return m_utf8;
    }
};

#endif
