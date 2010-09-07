#ifndef CONVERT_UTF8TO_UTF16_H_included
#define CONVERT_UTF8TO_UTF16_H_included

#include "WindowsError.h"

#include <string>

#if defined(__CYGWIN__) || defined(__MINGW32__)

#include <windows.h>

inline std::basic_string<WCHAR> convertUtf8ToUtf16(const std::string& input) {
    std::basic_string<WCHAR> output;
    // MultiByteToWideChar fails for empty input.
    if (input.empty()) {
        return output;
    }
    int rc = MultiByteToWideChar(CP_UTF8, 0, &input[0], input.size(), 0, 0);
    if (rc == 0) {
        DWORD lastError = GetLastError();
        throw WindowsError("initial, sizing, call to MultiByteToWideChar() failed", lastError);
    }
    output.resize(rc);
    rc = MultiByteToWideChar(CP_UTF8, 0, &input[0], input.size(), &output[0], output.size());
    if (rc == 0) {
        DWORD lastError = GetLastError();
        throw WindowsError("second, converting, call to MultiByteToWideChar() failed", lastError);
    }
    return output;
}

#endif

#endif
