#include "pty.h"

/* two file descriptors returned in fd[0] & fd[1] */
int s_pipe(int fd[2]) {
    return socketpair(AF_UNIX, SOCK_STREAM, 0, fd);
}

void do_driver(const char *driver) {
    /* create a stream pipe to communicate with the driver */
    int pipe[2];
    if (s_pipe(pipe) < 0) {
        panic("can't create stream pipe");
    }

    pid_t child = fork();
    if (child < 0) {
        panic("fork error");
    } else if (child == 0) {            /* child */
        close(pipe[1]);

        /* stdin for driver */
        if (dup2(pipe[0], STDIN_FILENO) != STDIN_FILENO) {
            panic("dup2 error to stdin");
        }

        /* stdout for driver */
        if (dup2(pipe[0], STDOUT_FILENO) != STDOUT_FILENO) {
            panic("dup2 error to stdout");
        }
        close(pipe[0]);

        /* leave stderr for driver alone */

        execlp(driver, driver, (char *) 0);
        panic("execlp error for", driver);
    }

    close(pipe[0]);        /* parent */

    if (dup2(pipe[1], STDIN_FILENO) != STDIN_FILENO) {
        panic("dup2 error to stdin");
    }

    if (dup2(pipe[1], STDOUT_FILENO) != STDOUT_FILENO) {
        panic("dup2 error to stdout");
    }
    close(pipe[1]);

    /* Parent returns, but with stdin and stdout connected
       to the driver. */
}
