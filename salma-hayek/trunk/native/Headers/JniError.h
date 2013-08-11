#ifndef JNI_ERROR_H_included
#define JNI_ERROR_H_included

#include "PortableJni.h"

#include <ostream>
#include <string>

/**
 * Wraps JNI function return values so that they can be easily converted to human-readable error messages.
 */
class JniError {
public:
    explicit JniError(int err) : m_err(err) {
    }
    
    void dumpTo(std::ostream& os) const {
        if (m_err == JNI_OK) {
            os << "JNI_OK (success)";
        } else if (m_err == JNI_ERR) {
            os << "JNI_ERR (unknown error)";
        } else if (m_err == JNI_EDETACHED) {
            os << "JNI_EDETACHED (thread detached from the VM)";
        } else if (m_err == JNI_EVERSION) {
            os << "JNI_EVERSION (JNI version error)";
        } else if (m_err == JNI_ENOMEM) {
            os << "JNI_ENOMEM (not enough memory)";
        } else if (m_err == JNI_EEXIST) {
            os << "JNI_EEXIST (VM already created)";
        } else if (m_err == JNI_EINVAL) {
            os << "JNI_EINVAL (invalid arguments)";
        } else {
            os << "undefined JNI error (" << m_err << ")";
        }
    }
    
private:
    int m_err;
};

std::ostream& operator<<(std::ostream& os, const JniError& rhs) {
    rhs.dumpTo(os);
    return os;
}

#endif
