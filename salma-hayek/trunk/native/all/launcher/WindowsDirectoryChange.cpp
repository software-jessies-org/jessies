#ifdef __CYGWIN__
#include <windows.h>
#endif

#include "WindowsDirectoryChange.h"

#include "toString.h"

#include <exception>

#if defined(__CYGWIN__)

struct WindowsError : std::runtime_error {
    WindowsError(const std::string& description)
    // FIXME: I know the magic to convert these numeric errors to strings.
    : std::runtime_error(description + " failed with Windows error code " + toString(GetLastError())) {
    }
};

std::string getCurrentWindowsDirectory() {
    std::string currentDirectory;
    currentDirectory.resize(MAX_PATH);
    DWORD rc = GetCurrentDirectory(currentDirectory.size(), &currentDirectory[0]);
    if (rc == 0) {
        throw WindowsError("GetCurrentDirectory()");
    }
    if (rc > currentDirectory.size()) {
        throw std::runtime_error("GetCurrentDirectory() claimed to want a buffer of " + toString(rc) + " when MAX_PATH is only " + toString(MAX_PATH));
    }
    // The returned size only includes the terminator if the buffer was too small.
    currentDirectory.resize(rc);
    return currentDirectory;
}

void setCurrentWindowsDirectory(const std::string& directory) {
    if (SetCurrentDirectory(directory.c_str()) == false) {
        throw WindowsError("SetCurrentDirectory(\"" + directory + "\")");
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
