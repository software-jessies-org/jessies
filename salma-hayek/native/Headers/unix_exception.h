#ifndef UNIX_EXCEPTION_H_included
#define UNIX_EXCEPTION_H_included

#include "strerror_r.h"

#include <errno.h>
#include <stdexcept>
#include <sstream>
#include <string.h>
#include <string>

class unix_exception : public std::runtime_error {
public:
    explicit unix_exception(const std::string& message)
    : std::runtime_error(message + (errno ? ": (" + errnoToString(errno) + ")" : " (but errno is zero)"))
    , m_errno(errno)
    {
    }
    
    int getErrno() const {
        return m_errno;
    }
    
    static std::string errnoToString(int errorNumber) {
        char messageBuffer[1024];
        if (gnuCompatibleStrerror(&strerror_r, errorNumber, &messageBuffer[0], sizeof(messageBuffer)) == -1) {
            int decodingError = errno;
            std::ostringstream os;
            switch (decodingError) {
            case EINVAL:
                os << "The value " << errorNumber << " is not a valid error number.";
                break;
            case ERANGE:
                os << sizeof(messageBuffer) << " bytes was not enough to contain the error description string for error number " << errorNumber << ".";
                break;
            default:
                os << "Decoding error number " << errorNumber << " produced error " << decodingError << ".";
                break;
            }
            return os.str();
        } else {
            return messageBuffer;
        }
    }
    
private:
    static int gnuCompatibleStrerror(int (*strerror_r_fn)(int, char*, size_t), int errorNumber, char* messageBuffer, size_t bufferSize) {
        return strerror_r_fn(errorNumber, messageBuffer, bufferSize);
    }
    
    static int gnuCompatibleStrerror(char* (*strerror_r_fn)(int, char*, size_t), int errorNumber, char* messageBuffer, size_t bufferSize) {
        const char* intermediateBuffer = strerror_r_fn(errorNumber, messageBuffer, bufferSize);
        // strncpy doesn't support copying over oneself.
        if (intermediateBuffer != messageBuffer) {
            strncpy(messageBuffer, intermediateBuffer, bufferSize);
            messageBuffer[bufferSize - 1] = 0;
        }
        return 0;
    }
    
    const int m_errno;
};

#endif
