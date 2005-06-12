#ifndef UNIX_EXCEPTION_H_included
#define UNIX_EXCEPTION_H_included

#include "errnoToString.h"

#include <stdexcept>

class UnixException : std::runtime_error {
public:
    UnixException(const std::string& str)
    : std::runtime_error(str + " failed" + errnoToString()) {
    }
};

#endif
