#include "pty.h"

static struct termios    save_termios;
static int                ttysavefd = -1;
static enum { RESET, RAW, CBREAK }    ttystate = RESET;

/* put terminal into a cbreak mode */
int tty_cbreak(int fd)
{
    if (tcgetattr(fd, &save_termios) < 0) {
        return -1;
    }

    struct termios buf;
    buf = save_termios;    /* structure copy */

    buf.c_lflag &= ~(ECHO | ICANON);
                    /* echo off, canonical mode off */

    buf.c_cc[VMIN] = 1;    /* Case B: 1 byte at a time, no timer */
    buf.c_cc[VTIME] = 0;

    if (tcsetattr(fd, TCSAFLUSH, &buf) < 0) {
        return -1;
    }
    ttystate = CBREAK;
    ttysavefd = fd;
    return 0;
}

/* put terminal into a raw mode */
int tty_raw(int fd) {
    if (tcgetattr(fd, &save_termios) < 0) {
        return -1;
    }

    struct termios buf;
    buf = save_termios;    /* structure copy */

    buf.c_lflag &= ~(ECHO | ICANON | IEXTEN | ISIG);
                    /* echo off, canonical mode off, extended input
                       processing off, signal chars off */

    buf.c_iflag &= ~(BRKINT | ICRNL | INPCK | ISTRIP | IXON);
                    /* no SIGINT on BREAK, CR-to-NL off, input parity
                       check off, don't strip 8th bit on input,
                       output flow control off */

    buf.c_cflag &= ~(CSIZE | PARENB);
                    /* clear size bits, parity checking off */
    buf.c_cflag |= CS8;
                    /* set 8 bits/char */

    buf.c_oflag &= ~(OPOST);
                    /* output processing off */

    buf.c_cc[VMIN] = 1;    /* Case B: 1 byte at a time, no timer */
    buf.c_cc[VTIME] = 0;

    if (tcsetattr(fd, TCSAFLUSH, &buf) < 0) {
        return -1;
    }
    ttystate = RAW;
    ttysavefd = fd;
    return 0;
}

/* restore terminal's mode */
int tty_reset(int fd) {
    if (ttystate != CBREAK && ttystate != RAW) {
        return 0;
    }

    if (tcsetattr(fd, TCSAFLUSH, &save_termios) < 0) {
        return -1;
    }
    ttystate = RESET;
    return 0;
}

/* can be set up by atexit(tty_atexit) */
void tty_atexit() {
    if (ttysavefd >= 0) {
        tty_reset(ttysavefd);
    }
}

/* let caller see original tty state */
struct termios* tty_termios()
{
    return &save_termios;
}
