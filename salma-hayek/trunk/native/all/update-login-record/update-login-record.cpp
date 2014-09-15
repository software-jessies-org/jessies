#include "updateLoginRecord.h"

#include "parseInteger.h"

#include <iostream>
#include <stdlib.h>

void throwUsage() {
    std::ostream& os = std::cerr;
    os << "Syntax: update-login-record <fd or -1> <pid> <slave pty name>";
    os << std::endl;
    exit(1);
}

int main(int, const char** argValues) {
    ++ argValues;
    if (*argValues == 0) {
        throwUsage();
    }
    int fd;
    if (parseInteger(*argValues, fd) == false) {
        throwUsage();
    }
    ++ argValues;
    if (*argValues == 0) {
        throwUsage();
    }
    pid_t pid;
    if (parseInteger(*argValues, pid) == false) {
        throwUsage();
    }
    ++ argValues;
    if (*argValues == 0) {
        throwUsage();
    }
    const char* slavePtyName = *argValues;
    ++ argValues;
    if (*argValues != 0) {
        throwUsage();
    }
    updateLoginRecord(fd, pid, slavePtyName);
}
