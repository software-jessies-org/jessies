// Fix signature of unsetenv
#define __DARWIN_UNIX03 1

#include <fcntl.h>
#include <iostream>
#include <stdlib.h>
#include <string>
#include <unistd.h>

#include "toString.h"
#include "unix_exception.h"
#include "WindowsDllErrorModeChange.h"

void usage() {
    std::ostream& os = std::cerr;
    os << "Usage: ruby-launcher <ruby interpreter> <ruby arguments>...";
    os << std::endl;
    exit(1);
}

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

int main(int, char** argValues) {
    int rc = unsetenv("RUBYOPT");
    if (rc != 0) {
        throw unix_exception("unsetenv(\"RUBYOPT\")");
    }
    ensureDescriptorIsOpen(1);
    ensureDescriptorIsOpen(2);
    ++ argValues;
    if (*argValues == 0) {
        usage();
    }
    const char* interpreter = *argValues;
    WindowsDllErrorModeChange windowsDllErrorModeChange;
    execvp(interpreter, argValues);
    throw unix_exception(std::string("execvp(\"") + interpreter + "\", ...)");
}
