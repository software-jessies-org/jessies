#ifndef WINDOWS_ERROR_H_included
#define WINDOWS_ERROR_H_included

#include "toString.h"

#include <exception>

#if defined(__CYGWIN__) || defined(__MINGW32__)

#include <windows.h>

struct WindowsError : std::runtime_error {
    WindowsError(const std::string& description)
    // FIXME: I know the magic to convert these numeric errors to strings.
    : std::runtime_error(description + " failed with Windows error code " + toString(GetLastError())) {
    }
};

#else

struct WindowsError : std::runtime_error {
    WindowsError(const std::string& description)
    : std::runtime_error(description) {
    }
};

#endif

#endif
