#include "pty.h"

typedef void SignalHandler(int);

SignalHandler* signal_intr(int signo, SignalHandler* func) {
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

void loop(int ptym)
{
    static const size_t BUFFER_SIZE = 32 * 1024;
    char buff[BUFFER_SIZE];
    
    int nread;
    int bytesInBuffer = 0;
    int bytesRequired = 1;
    
    pid_t child = fork();
    if (child < 0) {
        panic("fork error");
    } else if (child == 0) {    /* child copies stdin to ptym */
        for ( ; ; ) {
            if (bytesInBuffer < bytesRequired) {
                if ( (nread = read(STDIN_FILENO, buff + bytesInBuffer, sizeof(buff) - bytesInBuffer)) < 0)
                    panic("read error from stdin");
                else if (nread == 0)
                    break;        /* EOF on stdin means we're done */
                bytesInBuffer += nread;
            }

            if (buff[0] == SIZE_ESCAPE) {
                if (bytesInBuffer >= 2 && buff[1] == SIZE_ESCAPE) {
                    if (writen(ptym, buff, 1) != 1) {
                        panic("writen error to master pty");
                    }
                    bytesInBuffer -= 2;
                    memmove(buff, buff + 2, bytesInBuffer);
                    bytesRequired = 1;
                } else {
                    if (bytesInBuffer >= SIZE_STRUCT_SIZE) {  /* escapeLength + sizeof(short) * 4 */
                        struct winsize size;
                        size.ws_col = readUnsignedShort(buff + 2);
                        size.ws_row = readUnsignedShort(buff + 4);
                        size.ws_xpixel = readUnsignedShort(buff + 6);
                        size.ws_ypixel = readUnsignedShort(buff + 8);
                        if (ioctl(ptym, TIOCSWINSZ, (char *) &size) < 0) {
                            panic("TIOCSWINSZ error");
                        }
                        bytesInBuffer -= SIZE_STRUCT_SIZE;
                        memmove(buff, buff + SIZE_STRUCT_SIZE, bytesInBuffer);
                        bytesRequired = 1;
                    } else {
                        bytesRequired = SIZE_STRUCT_SIZE;
                    }
                }
            } else {
                for (int index = 0; index <= bytesInBuffer; ++index) {
                    if ((index == bytesInBuffer) || (buff[index] == SIZE_ESCAPE)) {
                        if (writen(ptym, buff, index) != index) {
                            panic("writen error to master pty");
                        }
                        bytesInBuffer = bytesInBuffer - index;  /* If there was no escape char, bytesInBuffer = 0, and 0 bytes copied. */
                        memmove(buff, buff + index, bytesInBuffer);
                        bytesRequired = 1;
                        break;
                    }
                }
            }
        }

        kill(getppid(), SIGTERM);    /* notify parent */
        exit(4);    /* and terminate; child can't return */
    }

        /* parent copies ptym to stdout */
    if (signal_intr(SIGTERM, sig_term) == SIG_ERR)
        panic("signal_intr error for SIGTERM");

    for ( ; ; ) {
        if ( (nread = read(ptym, buff, sizeof(buff))) <= 0)
            break;        /* signal caught, error, or EOF */

        if (writen(STDOUT_FILENO, buff, nread) != nread)
            panic("writen error to stdout");
    }

    /* There are three ways to get here: sig_term() below caught the
     * SIGTERM from the child, we read an EOF on the pty master (which
     * means we have to signal the child to stop), or an error. */

    if (sigcaught == 0)    /* tell child if it didn't send us the signal */
        kill(child, SIGTERM);
    return;        /* parent returns to caller */
}
