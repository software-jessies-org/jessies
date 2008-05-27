#ifdef __CYGWIN__
#include <windows.h>
#endif

// Fix signature of unsetenv
#define __DARWIN_UNIX03 1

#include <fcntl.h>
#include <string>
#include <unistd.h>

#include "reportFatalErrorViaGui.h"
#include "toString.h"
#include "unix_exception.h"
#include "WindowsDllErrorModeChange.h"

void ensureDescriptorIsOpen(int targetFd) {
    // This might look obscure but the man page suggests that it's a POSIX-compliant way
    // of testing whether a file descriptor is open.
    int rc = fcntl(targetFd, F_GETFL);
    if (rc != -1) {
        return;
    }
    if (errno != EBADF) {
        throw unix_exception("fcntl(" + toString(targetFd) + ", F_GETFL)");
    }
    int openedFd = open("/dev/null", O_WRONLY);
    if (openedFd == -1) {
        throw unix_exception("open(\"/dev/null\", O_WRONLY)");
    }
    // If we've already opened the right descriptor, we mustn't close it!
    if (openedFd == targetFd) {
        return;
    }
    rc = dup2(openedFd, targetFd);
    if (rc == -1) {
        throw unix_exception("dup2(" + toString(openedFd) + ", " + toString(targetFd) + ")");
    }
    rc = close(openedFd);
    if (rc == -1) {
        throw unix_exception("close(" + toString(openedFd) + ")");
    }
}

void launchRuby(char** argValues) {
    int rc = unsetenv("RUBYOPT");
    if (rc != 0) {
        throw unix_exception("unsetenv(\"RUBYOPT\")");
    }
    rc = setenv("RUBY_LAUNCHER_INVOKING", "1", 1);
    if (rc != 0) {
        throw unix_exception("setenv(\"RUBY_LAUNCHER_INVOKING\", \"1\", 1)");
    }
    ensureDescriptorIsOpen(1);
    ensureDescriptorIsOpen(2);
    if (*argValues == 0) {
        throw std::runtime_error("missing interpreter");
    }
    const char* interpreter = *argValues;
    WindowsDllErrorModeChange windowsDllErrorModeChange;
    execvp(interpreter, argValues);
    throw unix_exception(std::string("execvp(\"") + interpreter + "\", ...)");
}

int main(int, char** argValues) {
    const char* ARGV0 = *argValues;
    ++ argValues;
    try {
        // It would seem that we can, at the moment, but I expect it'll rust.
        //throw std::runtime_error("can we report an early failure?");
        launchRuby(argValues);
    } catch (const std::exception& ex) {
        std::ostringstream os;
        os << "Error: ";
        os << ex.what();
        os << std::endl;
        os << std::endl;
        os << "Usage: ";
        os << ARGV0;
        os << " <ruby interpreter> <ruby arguments>...";
        os << std::endl;
        reportFatalErrorViaGui("Ruby Launcher", os.str());
        return 1;
    }
}
