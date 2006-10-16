#include "clock.h"

char * display_string = 0;

/* Dispatcher for main event loop. */
typedef struct Disp Disp;
struct Disp {
    int    type;
    void    (*handler)(XEvent *);
};

static void update_display_string();
static void do_command(XEvent *);
static char * pipe_command(char *);
static void expose(XEvent *);
static void enter(XEvent *);
static void leave(XEvent *);
static void mappingnotify(XEvent *);
static void visibilitynotify(XEvent *);

static Disp disps[] = {
    {ButtonRelease, do_command},
    {Expose, expose},
    {EnterNotify, enter},
    {LeaveNotify, leave},
    {MappingNotify, mappingnotify},
    {VisibilityNotify, visibilitynotify},
};

extern void
dispatch(XEvent *ev) {
    Disp *p;

    for (p = disps; p < disps + sizeof disps / sizeof disps[0]; p++)
        if (p->type == ev->type) {
            if (p->handler != 0)
                p->handler(ev);
            return;
        }
}

static void
update_display_string() {
    if (display_string != 0)
        free(display_string);
    
    if (view_command != 0) {
        display_string = pipe_command(view_command);
    } else {
        /* Fall back to computing the date & time internally. */
        time_t t;
        struct tm tm;
        char time_string[17]; /* 1234-67-90 23:56 */
        
        time(&t);
        tm = *localtime(&t);
        strftime(time_string, sizeof time_string, "%Y-%m-%d %H:%M", &tm);
        
        display_string = sdup(time_string);
    }
    
    /* Recompute the window width. */
    window_width = 4 * XTextWidth(font, display_string,
        strlen(display_string)) / 3;
}

static void
expose(XEvent * ev) {
    int width;
    
    /* Only handle the last in a group of Expose events. */
    if ((ev && ev->xexpose.count != 0) || (display_string == 0))
        return;
    
    /* Do the redraw. */
    width = XTextWidth(font, display_string, strlen(display_string));
    XDrawString(dpy, window, gc, (window_width - width)/2,
        window_height / 4 + font->ascent,
        display_string, strlen(display_string));
}

static void
mappingnotify(XEvent * ev) {
    XRefreshKeyboardMapping((XMappingEvent *) ev);
}

static void
enter(XEvent * ev) {
    (void) ev;
    update_display_string();
    XMoveResizeWindow(dpy, window,
        sinister ? 0 : display_width-window_width, 0,
        window_width, window_height);
}

static void
leave(XEvent * ev) {
    (void) ev;
    XMoveResizeWindow(dpy, window,
        sinister ? 0 : display_width-2, 0, 2, 2);
}

static void
do_command(XEvent * ev) {
    int button = ev->xbutton.button;
    static char * sh;

    if (command[button] == 0)
        return;

    if (sh == 0) {
        sh = getenv("SHELL");
        if (sh == 0) sh = "/bin/sh";
    }

    switch (fork()) {
    case 0:    /* Child. */
        close(ConnectionNumber(dpy));
        switch (fork()) {
        case 0:
            execl(sh, sh, "-c", command[button], (char*) NULL);
            fprintf(stderr, "%s: can't exec \"%s -c %s\"\n", argv0, sh,
                command[button]);
            exit(EXIT_FAILURE);
        case -1:
            fprintf(stderr, "%s: couldn't fork\n", argv0);
            exit(EXIT_FAILURE);
        default:
            exit(EXIT_SUCCESS);
        }
    case -1:    /* Error. */
        fprintf(stderr, "%s: couldn't fork\n", argv0);
        break;
    default:
        wait(0);
    }
}

static char *
pipe_command(char * command) {
    static char * sh;
    int fds[2];
    char * string;
    
    if (sh == 0) {
        sh = getenv("SHELL");
        if (sh == 0) sh = "/bin/sh";
    }
    
    if (pipe(fds) == -1)
        return "Execution failed";
    
    switch (fork()) {
    case 0:    /* Child. */
        close(ConnectionNumber(dpy));
        switch (fork()) {
        case 0:
            close(0);
            close(fds[0]);
            dup2(fds[1], 1);
            dup2(1, 2);
            
            execl(sh, sh, "-c", command, (char*) NULL);
            fprintf(stderr, "%s: can't exec \"%s -c %s\"\n", argv0, sh,
                command);
            exit(EXIT_FAILURE);
        case -1:
            fprintf(stderr, "%s: couldn't fork\n", argv0);
            exit(EXIT_FAILURE);
        default:
            exit(EXIT_SUCCESS);
        }
    case -1:    /* Error. */
        fprintf(stderr, "%s: couldn't fork\n", argv0);
        break;
    default: {
        /* Read from the pipe. */
        char buf[BUFSIZ];
        int n;
        
        close(fds[1]);
        
        while ((n = read(fds[0], buf, BUFSIZ)) > 0) {
            int valid = 0;
            char * p = buf;

            /* How many of the characters do we want? */
            while (*p++ >= ' ') valid++;
            
            string = (char *) malloc(valid + 1);
            if (string != 0)
                strncpy(string, buf, valid);
            else
                string = "Out of memory";
        }
        
        wait(0);
        }
    }
    
    return string;
}

static void
visibilitynotify(XEvent * ev) {
    char * name;
    Window root_return, parent_return;
    Window * wins;
    unsigned int nwins;

    if(ev->xvisibility.state == VisibilityUnobscured)
        return;

    /* Is it lock? */
    XQueryTree(dpy, root, &root_return, &parent_return, &wins, &nwins);
    XFetchName(dpy, wins[nwins-1], &name);
    if(name == 0 || strcmp(name, "lock"))
        XRaiseWindow(dpy, window);

    XFree(wins);
    XFree(name);
}
