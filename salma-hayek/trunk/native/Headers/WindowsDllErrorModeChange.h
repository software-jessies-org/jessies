#ifndef WINDOWS_DLL_ERROR_MODE_CHANGE_H_included
#define WINDOWS_DLL_ERROR_MODE_CHANGE_H_included

#if defined(__CYGWIN__) || defined(__MINGW32__)

#include <windows.h>

struct WindowsDllErrorModeChange {
private:
    UINT previousErrorMode;
    
public:
    WindowsDllErrorModeChange()
    : previousErrorMode(SetErrorMode(0)) {
    }
    
    ~WindowsDllErrorModeChange() {
        SetErrorMode(previousErrorMode);
    }
};

#else

struct WindowsDllErrorModeChange {
    // Stop the unused variable warning.
    WindowsDllErrorModeChange() {
    }
};

#endif

#endif
