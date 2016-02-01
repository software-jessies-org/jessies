#include "WindowsDirectoryChange.h"

#include "convertUtf16ToUtf8.h"
#include "convertUtf8ToUtf16.h"
#include "toString.h"
#include "WindowsError.h"

#if defined(__CYGWIN__) || defined(__MINGW32__)

#include <windows.h>

std::string getCurrentWindowsDirectory() {
    std::basic_string<WCHAR> currentDirectoryInUtf16;
    currentDirectoryInUtf16.resize(MAX_PATH);
    DWORD rc = GetCurrentDirectoryW(currentDirectoryInUtf16.size(), &currentDirectoryInUtf16[0]);
    if (rc == 0) {
        DWORD lastError = GetLastError();
        throw WindowsError("GetCurrentDirectory()", lastError);
    }
    if (rc > currentDirectoryInUtf16.size()) {
        throw std::runtime_error("GetCurrentDirectory() claimed to want a buffer of " + toString(rc) + " when MAX_PATH is only " + toString(MAX_PATH));
    }
    // The returned size only includes the terminator if the buffer was too small.
    currentDirectoryInUtf16.resize(rc);
    std::string currentDirectoryUtf8 = convertUtf16ToUtf8(currentDirectoryInUtf16);
    return currentDirectoryUtf8;
}

void setCurrentWindowsDirectory(const std::string& directoryInUtf8) {
    // SetCurrentDirectory wants a trailing backslash.
    // Usually, it will add that for you, but perhaps not when the directory name starts with \\?.
    // At least, not on newer versions of Windows than XP.
    std::ostringstream os;
    os << directoryInUtf8;
    if (directoryInUtf8.empty() || directoryInUtf8[directoryInUtf8.size() - 1] != '\\') {
        os << '\\';
    }
    std::string canonicalizedDirectoryInUtf8 = os.str();
    std::basic_string<WCHAR> directoryInUtf16 = convertUtf8ToUtf16(canonicalizedDirectoryInUtf8);
    if (SetCurrentDirectoryW(directoryInUtf16.c_str()) == false) {
        DWORD lastError = GetLastError();
        throw WindowsError("SetCurrentDirectory(\"" + canonicalizedDirectoryInUtf8 + "\")", lastError);
    }
}

#else

std::string getCurrentWindowsDirectory() {
    return std::string();
}

void setCurrentWindowsDirectory(const std::string&) {
}

#endif

WindowsDirectoryChange::WindowsDirectoryChange(const std::string& targetDirectory)
: previousDirectory(getCurrentWindowsDirectory()) {
    setCurrentWindowsDirectory(targetDirectory);
}

WindowsDirectoryChange::~WindowsDirectoryChange() {
    setCurrentWindowsDirectory(previousDirectory);
}
