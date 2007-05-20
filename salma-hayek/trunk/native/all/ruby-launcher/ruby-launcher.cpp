// Fix signature of unsetenv
#define __DARWIN_UNIX03 1

#include <iostream>
#include <stdlib.h>
#include <string>
#include <unistd.h>

#include "unix_exception.h"

void usage() {
    std::ostream& os = std::cerr;
    os << "Usage: ruby-launcher <ruby interpreter> <ruby arguments>...";
    os << std::endl;
    exit(1);
}

int main(int, char** argValues) {
    int rc = unsetenv("RUBYOPT");
    if (rc != 0) {
        throw unix_exception("unsetenv(\"RUBYOPT\")");
    }
    ++ argValues;
    if (*argValues == 0) {
        usage();
    }
    const char* interpreter = *argValues;
    execvp(interpreter, argValues);
    throw unix_exception(std::string("execvp(\"") + interpreter + "\", ...)");
}
