#ifndef PTY_H_included
#define PTY_H_included

#include <fcntl.h>
#include <grp.h>
#include <termios.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>

#include <errno.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define MAXLINE 4096   /* max line length */

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

int ptym_open(char *);
int ptys_open(int, char *);
#ifdef TIOCGWINSZ
pid_t pty_fork(int *, char *, const struct termios *, const struct winsize *);
#endif

void err_quit(const char *, ...);
void err_sys(const char *, ...);

#endif
