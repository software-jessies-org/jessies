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

void loop(int ptym, int ignoreeof)
{
    int        nread;
    char    buff[BUFFSIZE];
    pid_t    child;
    if ( (child = fork()) < 0) {
        err_sys("fork error");

    } else if (child == 0) {    /* child copies stdin to ptym */
        for ( ; ; ) {
            if ( (nread = read(STDIN_FILENO, buff, BUFFSIZE)) < 0)
                err_sys("read error from stdin");
            else if (nread == 0)
                break;        /* EOF on stdin means we're done */

            if (writen(ptym, buff, nread) != nread)
                err_sys("writen error to master pty");
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
