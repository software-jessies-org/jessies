#include "pty.h"

extern void loop(int);

int main(int, char* argv[]) {
    int fdm;
    pid_t pid = pty_fork(&fdm);
    if (pid < 0) {
        panic("fork error");
    } else if (pid == 0) {
        /* child */
        putenv("TERM=terminator");
        
        /*
         * rxvt resets these signal handlers, and we'll do the same, because it magically
         * fixes the bug where ^c doesn't work if we're launched from KDE or Gnome's
         * launcher program.  I don't quite understand why - maybe bash reads the existing
         * SIGINT setting, and if it's set to something other than DFL it lets the parent process
         * take care of job control.
         */
        signal(SIGINT, SIG_DFL);
        signal(SIGQUIT, SIG_DFL);
        signal(SIGCHLD, SIG_DFL);

        if (execvp(argv[1], &argv[1]) < 0) {
            panic("can't execute", argv[1]);
        }
    }

    loop(fdm);    /* copies stdin -> ptym, ptym -> stdout */
    
    {
        int p;
        int status = 0;
        do {
            errno = 0;
        } while ((p = waitpid(-1, &status, WNOHANG)) == -1 && errno == EINTR);
        exit(WIFEXITED(status) ? WEXITSTATUS(status) : WTERMSIG(status));
    }
}
