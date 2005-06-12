#ifndef JNI_FIELD_H_included
#define JNI_FIELD_H_included

#include <jni.h>
#include <sstream>
#include <stdexcept>
#include <string>

/**
 * Hides the details of accessing Java fields via JNI.
 */
template <typename NativeT>
class JniField {
    typedef JniField<NativeT> Self;
    
    JNIEnv* m_env;
    jobject m_instance;
    const char* m_fieldName;
    const char* m_fieldSignature;
    
public:
    // Creates a proxy for an instance field.
    // FIXME: should be able to proxy static fields.
    JniField(JNIEnv* env, jobject instance, const char* name, const char* signature)
    : m_env(env)
    , m_instance(instance)
    , m_fieldName(name)
    , m_fieldSignature(signature)
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
        // It's not a valid optimization to cache field ids in face of class
        // unloading. We could keep a global reference to the class to prevent
        // it being unloaded, but that seems unfriendly.
        jclass objectClass = m_env->GetObjectClass(m_instance);
        jfieldID result = m_env->GetFieldID(objectClass, m_fieldName, m_fieldSignature);
        if (result == 0) {
            std::ostringstream message;
            message << "couldn't find field " << m_fieldName << " (" << m_fieldSignature << ")";
            throw std::runtime_error(message.str());
        }
        return result;
    }
};

template <> void JniField<jobject>::set(const jobject& rhs) {
    m_env->SetObjectField(m_instance, getFieldID(), rhs);
}
template <> void JniField<jboolean>::set(const jboolean& rhs) {
    m_env->SetBooleanField(m_instance, getFieldID(), rhs);
}
template <> void JniField<jbyte>::set(const jbyte& rhs) {
    m_env->SetByteField(m_instance, getFieldID(), rhs);
}
template <> void JniField<jchar>::set(const jchar& rhs) {
    m_env->SetCharField(m_instance, getFieldID(), rhs);
}
template <> void JniField<jshort>::set(const jshort& rhs) {
    m_env->SetShortField(m_instance, getFieldID(), rhs);
}
template <> void JniField<jint>::set(const jint& rhs) {
    m_env->SetIntField(m_instance, getFieldID(), rhs);
}
template <> void JniField<jlong>::set(const jlong& rhs) {
    m_env->SetLongField(m_instance, getFieldID(), rhs);
}
template <> void JniField<jfloat>::set(const jfloat& rhs) {
    m_env->SetFloatField(m_instance, getFieldID(), rhs);
}
template <> void JniField<jdouble>::set(const jdouble& rhs) {
    m_env->SetDoubleField(m_instance, getFieldID(), rhs);
}

template <> void JniField<jobject>::get(jobject& result) const {
    result = m_env->GetObjectField(m_instance, getFieldID());
}
template <> void JniField<jboolean>::get(jboolean& result) const {
    result = m_env->GetBooleanField(m_instance, getFieldID());
}
template <> void JniField<jbyte>::get(jbyte& result) const {
    result = m_env->GetByteField(m_instance, getFieldID());
}
template <> void JniField<jchar>::get(jchar& result) const {
    result = m_env->GetCharField(m_instance, getFieldID());
}
template <> void JniField<jshort>::get(jshort& result) const {
    result = m_env->GetShortField(m_instance, getFieldID());
}
template <> void JniField<jint>::get(jint& result) const {
    result = m_env->GetIntField(m_instance, getFieldID());
}
template <> void JniField<jlong>::get(jlong& result) const {
    result = m_env->GetLongField(m_instance, getFieldID());
}
template <> void JniField<jfloat>::get(jfloat& result) const {
    result = m_env->GetFloatField(m_instance, getFieldID());
}
template <> void JniField<jdouble>::get(jdouble& result) const {
    result = m_env->GetDoubleField(m_instance, getFieldID());
}

#endif
