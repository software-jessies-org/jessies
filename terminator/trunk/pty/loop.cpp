#include    "pty.h"

#define    BUFFSIZE    512

Sigfunc * signal_intr(int signo, Sigfunc *func) {
    struct sigaction act, oact;
    act.sa_handler = func;
    sigemptyset(&act.sa_mask);
    act.sa_flags = 0;
#ifdef SA_INTERRUPT /* SunOS */
    act.sa_flags |= SA_INTERRUPT;
#endif
    if (sigaction(signo, &act, &oact) < 0) {
        return SIG_ERR;
    }
    return oact.sa_handler;
}

static volatile sig_atomic_t    sigcaught;    /* set by signal handler */

/**
 * The child sends us a SIGTERM when it receives an EOF on
 * the pty slave or encounters a read() error.
 */
static void sig_term(int signo)
{
    (void) signo;
    sigcaught = 1;        /* just set flag and return */
    return;                /* probably interrupts read() of ptym */
}

/* Write "n" bytes to a descriptor. */
ssize_t writen(int fd, const void *vptr, size_t n) {
    const char* ptr = reinterpret_cast<const char*>(vptr); /* can't do pointer arithmetic on void* */
    size_t nleft = n;
    while (nleft > 0) {
        size_t nwritten;
        if ( (nwritten = write(fd, ptr, nleft)) <= 0) {
            return nwritten;        /* error */
        }

        nleft -= nwritten;
        ptr   += nwritten;
    }
    return n;
}

unsigned short readUnsignedShort(char *buf) {
    unsigned short result = ((buf[0] << 8) & 0xff00) | (buf[1] & 0xff);
    return result;
}

void loop(int ptym, int ignoreeof)
{
    int        nread;
    char    buff[BUFFSIZE];
    pid_t    child;
    int index;
    int bytesInBuffer = 0;
    int bytesRequired = 1;
    if ( (child = fork()) < 0) {
        err_sys("fork error");

    } else if (child == 0) {    /* child copies stdin to ptym */
        for ( ; ; ) {
            if (bytesInBuffer < bytesRequired) {
                if ( (nread = read(STDIN_FILENO, buff + bytesInBuffer, BUFFSIZE - bytesInBuffer)) < 0)
                    err_sys("read error from stdin");
                else if (nread == 0)
                    break;        /* EOF on stdin means we're done */
                bytesInBuffer += nread;
            }

            if (buff[0] == SIZE_ESCAPE) {
                if (bytesInBuffer >= 2 && buff[1] == SIZE_ESCAPE) {
                    if (writen(ptym, buff, 1) != 1) {
                        err_sys("writen error to master pty");
                    }
                    bytesInBuffer -= 2;
                    memcpy(buff, buff + 2, bytesInBuffer);
                    bytesRequired = 1;
                } else {
                    if (bytesInBuffer >= SIZE_STRUCT_SIZE) {  /* escapeLength + sizeof(short) * 4 */
                        struct winsize size;
                        size.ws_row = readUnsignedShort(buff + 2);
                        size.ws_col = readUnsignedShort(buff + 4);
                        size.ws_xpixel = readUnsignedShort(buff + 6);
                        size.ws_ypixel = readUnsignedShort(buff + 8);
                        if (ioctl(ptym, TIOCGWINSZ, (char *) &size) < 0) {
                            err_sys("TIOCGWINSZ error");
                        }
                        bytesInBuffer -= SIZE_STRUCT_SIZE;
                        memcpy(buff, buff + SIZE_STRUCT_SIZE, bytesInBuffer);
                        bytesRequired = 1;
                    } else {
                        bytesRequired = SIZE_STRUCT_SIZE;
                    }
                }
            } else {
                for (index = 0; index <= bytesInBuffer; index++) {
                    if ((index == bytesInBuffer) || (buff[index] == SIZE_ESCAPE)) {
                        if (writen(ptym, buff, index) != index) {
                            err_sys("writen error to master pty");
                        }
                        bytesInBuffer = bytesInBuffer - index;  /* If there was no escape char, bytesInBuffer = 0, and 0 bytes copied. */
                        memcpy(buff, buff + index, bytesInBuffer);
                        bytesRequired = 1;
                        break;
                    }
                }
            }
        }

            /* We always terminate when we encounter an EOF on stdin,
               but we only notify the parent if ignoreeof is 0. */
        if (ignoreeof == 0)
            kill(getppid(), SIGTERM);    /* notify parent */
        exit(4);    /* and terminate; child can't return */
    }

        /* parent copies ptym to stdout */
    if (signal_intr(SIGTERM, sig_term) == SIG_ERR)
        err_sys("signal_intr error for SIGTERM");

    for ( ; ; ) {
        if ( (nread = read(ptym, buff, BUFFSIZE)) <= 0)
            break;        /* signal caught, error, or EOF */

        if (writen(STDOUT_FILENO, buff, nread) != nread)
            err_sys("writen error to stdout");
    }

    /* There are three ways to get here: sig_term() below caught the
     * SIGTERM from the child, we read an EOF on the pty master (which
     * means we have to signal the child to stop), or an error. */

    if (sigcaught == 0)    /* tell child if it didn't send us the signal */
        kill(child, SIGTERM);
    return;        /* parent returns to caller */
}
