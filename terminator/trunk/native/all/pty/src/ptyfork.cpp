#include "pty.h"

pid_t pty_fork(int* ptrfdm) {
    std::string pty_name;
    int fdm = ptym_open(pty_name);

    pid_t pid = fork();
    if (pid < 0) {
        return -1;
    } else if (pid == 0) {
        /* child */
        if (setsid() < 0)
            panic("setsid error");

        /* SVR4 acquires controlling terminal on open() */
        int fds = ptys_open(pty_name);
        if (fds < 0)
            panic("can't open slave pty");
        close(fdm);        /* all done with master in child */

#if    defined(TIOCSCTTY) && !defined(CIBAUD)
                /* 44BSD way to acquire controlling terminal */
                /* !CIBAUD to avoid doing this under SunOS */
        if (ioctl(fds, TIOCSCTTY, (char *) 0) < 0)
            panic("TIOCSCTTY error");
#endif
                /* slave becomes stdin/stdout/stderr of child */
        if (dup2(fds, STDIN_FILENO) != STDIN_FILENO)
            panic("dup2 error to stdin");
        if (dup2(fds, STDOUT_FILENO) != STDOUT_FILENO)
            panic("dup2 error to stdout");
        if (dup2(fds, STDERR_FILENO) != STDERR_FILENO)
            panic("dup2 error to stderr");
        if (fds > STDERR_FILENO)
            close(fds);
        return(0);        /* child returns 0 just like fork() */
    } else {
        /* parent */
        *ptrfdm = fdm;    /* return fd of master */
        return(pid);    /* parent returns pid of child */
    }
}
