#ifndef PTY_H_included
#define PTY_H_included

#include <fcntl.h>
#include <grp.h>
#include <signal.h>
#include <termios.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <iostream>
#include <sstream>
#include <string>

#define SIZE_STRUCT_SIZE 10  /* 2 (escapeSequenceLength) + 4 * sizeof(unsigned short). */
#define SIZE_ESCAPE 0    /* Magic character used to escape size change notifications */

int ptym_open(std::string&);
int ptys_open(const std::string&);
pid_t pty_fork(int*);

inline void panic(const char* reason, const char* parameter = 0) {
    std::ostream& os(std::cout);
    os << reason;
    if (parameter != 0) {
        os << " '" << parameter << "'";
    }
    if (errno != 0) {
        os << ": " << strerror(errno);
    }
    os << std::endl;
    exit(1);
}

#endif
