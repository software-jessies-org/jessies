#ifndef PTY_H_included
#define PTY_H_included

#include <fcntl.h>
#include <grp.h>
#include <iostream>
#include <string>
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

#define SIZE_STRUCT_SIZE 10  /* 2 (escapeSequenceLength) + 4 * sizeof(unsigned short). */
#define SIZE_ESCAPE 0    /* Magic character used to escape size change notifications */

typedef void Sigfunc(int); /* for signal handlers */

     /* 4.3BSD Reno <signal.h> doesn't define SIG_ERR */
#if defined(SIG_IGN) && !defined(SIG_ERR)
#define SIG_ERR ((Sigfunc *)-1)
#endif

Sigfunc *signal_intr(int, Sigfunc *);/* {Prog signal_intr_function} */

int tty_cbreak(int);
int tty_raw(int);
int tty_reset(int);
void tty_atexit(void);
#ifdef ECHO
struct termios *tty_termios(void);
#endif

int ptym_open(std::string&);
int ptys_open(int, const char *);
#ifdef TIOCGWINSZ
pid_t pty_fork(int *, char *, const struct termios *, const struct winsize *);
#endif

inline void panic(const char* reason, const char* parameter = 0) {
    std::cerr << reason;
    if (parameter != 0) {
        std::cerr << " '" << parameter << "'";
    }
    std::cerr << ": " << strerror(errno) << std::endl;;
    exit(1);
}

#endif
