#ifndef JNI_FIELD_H_included
#define JNI_FIELD_H_included

#include "PortableJni.h"

#include <sstream>
#include <stdexcept>
#include <string>

/**
 * Hides the details of accessing Java fields via JNI.
 */
template <typename NativeT, bool isStatic>
class JniField {
    typedef JniField<NativeT, isStatic> Self;
    
    JNIEnv* m_env;
    jobject m_instance;
    const char* m_fieldName;
    const char* m_fieldSignature;
    
public:
    // Creates a proxy for a field.
    JniField(JNIEnv* env, jobject instance, const char* name, const char* signature)
    : m_env(env)
    , m_instance(instance)
    , m_fieldName(name)
    , m_fieldSignature(signature)
    {
    }
    
    // Used by JavaHpp.java to prevent static methods trying to access non-static fields.
    JniField()
    : m_env(0), m_instance(0), m_fieldName(0), m_fieldSignature(0)
    {
    }
    
    Self& operator=(const NativeT& rhs) {
        set(rhs);
        return *this;
    }
    
    NativeT get() const {
        NativeT result;
        get(result);
        return result;
    }
    
private:
    void get(NativeT&) const;
    void set(const NativeT&);
    
    jfieldID getFieldID() const {
        // It's not a valid optimization to cache field ids in face of class unloading.
        // We could keep a global reference to the class to prevent it being unloaded, but that seems unfriendly.
        jfieldID result = isStatic ? m_env->GetStaticFieldID(getObjectClass(), m_fieldName, m_fieldSignature) : m_env->GetFieldID(getObjectClass(), m_fieldName, m_fieldSignature);
        if (result == 0) {
            std::ostringstream message;
            message << "couldn't find field " << m_fieldName << " (" << m_fieldSignature << ")";
            throw std::runtime_error(message.str());
        }
        return result;
    }
    
    jclass getObjectClass() const {
        // The JNI specification (http://java.sun.com/j2se/1.5.0/docs/guide/jni/spec/functions.html) suggests that GetObjectClass can't fail, so we don't need to check for exceptions.
        return m_env->GetObjectClass(m_instance);
    }
};

#define org_jessies_JniField_ACCESSORS(TYPE, FUNCTION_NAME_FRAGMENT) \
    template <> void JniField<TYPE, true>::set(const TYPE& rhs) { \
        m_env->SetStatic ## FUNCTION_NAME_FRAGMENT ## Field(getObjectClass(), getFieldID(), rhs); \
    } \
    template <> void JniField<TYPE, false>::set(const TYPE& rhs) { \
        m_env->Set ## FUNCTION_NAME_FRAGMENT ## Field(m_instance, getFieldID(), rhs); \
    } \
    template <> void JniField<TYPE, true>::get(TYPE& result) const { \
        result = (TYPE) m_env->GetStatic ## FUNCTION_NAME_FRAGMENT ## Field(getObjectClass(), getFieldID()); \
    } \
    template <> void JniField<TYPE, false>::get(TYPE& result) const { \
        result = (TYPE) m_env->Get ## FUNCTION_NAME_FRAGMENT ## Field(m_instance, getFieldID()); \
    }

org_jessies_JniField_ACCESSORS(jstring, Object)
org_jessies_JniField_ACCESSORS(jobject, Object)
org_jessies_JniField_ACCESSORS(jboolean, Boolean)
org_jessies_JniField_ACCESSORS(jbyte, Byte)
org_jessies_JniField_ACCESSORS(jchar, Char)
org_jessies_JniField_ACCESSORS(jshort, Short)
org_jessies_JniField_ACCESSORS(jint, Int)
org_jessies_JniField_ACCESSORS(jlong, Long)
org_jessies_JniField_ACCESSORS(jfloat, Float)
org_jessies_JniField_ACCESSORS(jdouble, Double)

#undef org_jessies_JniField_ACCESSORS

#endif
