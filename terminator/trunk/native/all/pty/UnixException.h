#ifndef UNIX_EXCEPTION_H_included
#define UNIX_EXCEPTION_H_included

#include "errnoToString.h"

#include <stdexcept>

class UnixException : public std::runtime_error {
public:
    UnixException(const std::string& message)
    : std::runtime_error(message + " failed" + (errno ? ": (" + errnoToString() + ")" : "")) {
    }
};

#endif
