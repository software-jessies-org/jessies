#ifndef CONVERT_UTF16TO_UTF8_H_included
#define CONVERT_UTF16TO_UTF8_H_included

#include "WindowsError.h"

#include <string>

#if defined(__CYGWIN__) || defined(__MINGW32__)

#include <windows.h>

inline std::string convertUtf16ToUtf8(const std::basic_string<WCHAR>& input) {
    std::string output;
    // WideChartoMultiByte fails for empty input.
    if (input.empty()) {
        return output;
    }
    int rc = WideCharToMultiByte(CP_UTF8, 0, &input[0], input.size(), 0, 0, 0, 0);
    if (rc == 0) {
        DWORD lastError = GetLastError();
        throw WindowsError("initial, sizing, call to WideCharToMultiByte() failed", lastError);
    }
    output.resize(rc);
    rc = WideCharToMultiByte(CP_UTF8, 0, &input[0], input.size(), &output[0], output.size(), 0, 0);
    if (rc == 0) {
        DWORD lastError = GetLastError();
        throw WindowsError("second, converting, call to WideCharToMultiByte() failed", lastError);
    }
    return output;
}

#endif

#endif
