#include    "pty.h"

static void    set_noecho(int);    /* at the end of this file */
void        do_driver(const char *);    /* in the file driver.c */
void        loop(int, int);        /* in the file loop.c */

int
main(int argc, char *argv[])
{
    int                fdm, ignoreeof, interactive, noecho, verbose;
    pid_t            pid;
    char            *driver, slave_name[20];
    struct termios    orig_termios;
    struct winsize    size;
    
    argc = argc;  /* Stop gcc complaining. */

    interactive = isatty(STDIN_FILENO);
    ignoreeof = 0;
    noecho = 0;
    verbose = 0;
    driver = NULL;

    if (interactive) {    /* fetch current termios and window size */
        if (tcgetattr(STDIN_FILENO, &orig_termios) < 0)
            panic("tcgetattr error on stdin");
        if (ioctl(STDIN_FILENO, TIOCGWINSZ, (char *) &size) < 0)
            panic("TIOCGWINSZ error");
        pid = pty_fork(&fdm, slave_name, &orig_termios, &size);

    } else
        pid = pty_fork(&fdm, slave_name, NULL, NULL);

    if (pid < 0)
        panic("fork error");

    else if (pid == 0) {        /* child */
        if (noecho)
            set_noecho(STDIN_FILENO);    /* stdin is slave pty */
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

        if (execvp(argv[1], &argv[1]) < 0)
            panic("can't execute", argv[1]);
    }

    if (verbose) {
        fprintf(stderr, "slave name = %s\n", slave_name);
        if (driver != NULL)
            fprintf(stderr, "driver = %s\n", driver);
    }

    if (interactive && driver == NULL) {
        if (tty_raw(STDIN_FILENO) < 0)    /* user's tty to raw mode */
            panic("tty_raw error");
        if (atexit(tty_atexit) < 0)        /* reset user's tty on exit */
            panic("atexit error");
    }

    if (driver)
        do_driver(driver);    /* changes our stdin/stdout */

    loop(fdm, ignoreeof);    /* copies stdin -> ptym, ptym -> stdout */
    
    {
            int             p;
            int status = 0;
            
            do {
                    errno = 0;
            } while ((p = waitpid(-1, &status, WNOHANG)) == -1 && errno == EINTR);
            //*(int*)0 = 0;
            exit(WIFEXITED(status) ? WEXITSTATUS(status) : WTERMSIG(status));
    }
}
static void
set_noecho(int fd)        /* turn off echo (for slave pty) */
{
    struct termios    stermios;

    if (tcgetattr(fd, &stermios) < 0)
        panic("tcgetattr error");

    stermios.c_lflag &= ~(ECHO | ECHOE | ECHOK | ECHONL);
    stermios.c_oflag &= ~(ONLCR);
            /* also turn off NL to CR/NL mapping on output */

    if (tcsetattr(fd, TCSANOW, &stermios) < 0)
        panic("tcsetattr error");
}
