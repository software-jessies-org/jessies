#ifndef WINDOWS_ERROR_H_included
#define WINDOWS_ERROR_H_included

#include "toString.h"

#include <exception>

#if defined(__CYGWIN__) || defined(__MINGW32__)

#include <windows.h>

struct WindowsError : std::runtime_error {
private:
    // FIXME: I know the FormatMessage magic to convert these numeric errors to strings.
    static std::string makeErrorMessage(const std::string& description, DWORD errorCode) {
        return description + " failed with Windows error code " + toString(errorCode);
    }
    
public:
    WindowsError(const std::string& description)
    : std::runtime_error(makeErrorMessage(description, GetLastError())) {
    }
    WindowsError(const std::string& description, DWORD errorCode)
    : std::runtime_error(makeErrorMessage(description, errorCode)) {
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
