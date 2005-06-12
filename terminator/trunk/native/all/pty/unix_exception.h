#ifndef UNIX_EXCEPTION_H_included
#define UNIX_EXCEPTION_H_included

#include "errnoToString.h"

#include <stdexcept>

class unix_exception : public std::runtime_error {
public:
    unix_exception(const std::string& message)
    : std::runtime_error(message + " failed" + (errno ? ": (" + errnoToString() + ")" : "")) {
    }
};

#endif
