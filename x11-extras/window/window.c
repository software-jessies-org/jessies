#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <errno.h>

#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>

#include <X11/X.h>
#include <X11/Xos.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/Xatom.h>

typedef struct ReasonDesc ReasonDesc;
struct ReasonDesc {
    char    *name;
    int    nargs;
    void    (*fn)(char**);
    char    *usage;
};

Display *dpy;
char    *argv0;

static Window
wind(char *p) {
    long    l;
    char    *endp;
    
    if (!strcmp(p, "root")) {
        return DefaultRootWindow(dpy);
    }
    
    l = strtol(p, &endp, 0);
    if (*p != '\0' && *endp == '\0') {
        return (Window) l;
    }
    fprintf(stderr, "%s: %s is not a valid window id\n", argv0, p);
    exit(EXIT_FAILURE);
}

static int
listwindows(Window w, Window **ws) {
    unsigned    nkids;
    Window    dumb;

    XQueryTree(dpy, w, &dumb, &dumb, ws, &nkids);
    return nkids;
}

static void    
RaiseWindow(char **argv) {
    XRaiseWindow(dpy, wind(*argv));
}

static void    
LowerWindow(char **argv) {
    XLowerWindow(dpy, wind(*argv));
}

static void    
KillWindow(char **argv) {
    XKillClient(dpy, wind(*argv));
}

static void    
HideWindow(char **argv) {
    XEvent    ev;
    Window    root;
    Atom    wm_change_state;

    root = DefaultRootWindow(dpy);
    wm_change_state = XInternAtom(dpy, "WM_CHANGE_STATE", False);

    memset(&ev, '\0', sizeof ev);
    ev.xclient.type = ClientMessage;
    ev.xclient.window = wind(*argv);
    ev.xclient.message_type = wm_change_state;
    ev.xclient.format = 32;
    ev.xclient.data.l[0] = IconicState;
    ev.xclient.data.l[1] = CurrentTime;

    XSendEvent(dpy, root, False, SubstructureRedirectMask, &ev);
}

static void    
UnhideWindow(char **argv) {
    XMapWindow(dpy, wind(*argv));
}

static void    
printwindowname(Window w) {
    unsigned char    *name;
    Atom     actual_type;
    int    format;
    unsigned long    n;
    unsigned long    extra;

    /*
     *      This rather unpleasant hack is necessary because xwsh uses
     *      COMPOUND_TEXT rather than STRING for its WM_NAME property,
     *      and anonymous xwsh windows are annoying.
     */
    if(Success == XGetWindowProperty(dpy, w, XA_WM_NAME, 0L, 100L, False, AnyPropertyType,
            &actual_type, &format, &n, &extra, &name) && name != 0)
        printf("%#x\t%s\n", (unsigned)w, (char*)name);
    else
        printf("%#x\t%s\n", (unsigned)w, "(none)");

    if(name)
        XFree(name);
}

static void
LabelWindow(char **argv) {
    Window    w;

    w = wind(argv[0]);
    XStoreName(dpy, w, argv[1]);
    XSetIconName(dpy, w, argv[1]);
}

/*
 *    This can only give us an approximate geometry since
 *    we can't find out what decoration the window has.
 */
static void
WhereWindow(char **argv) {
    Window root;
    int x, y;
    unsigned w, h, border, depth;

    XGetGeometry(dpy, wind(*argv), &root, &x, &y, &w, &h, &border, &depth);
    printf("-geometry %ux%u+%u+%u\n", w, h, x, y);
}

static void
MoveWindow(char **argv) {
       XMoveWindow(dpy, wind(argv[0]), atoi(argv[1]), atoi(argv[2]));
}

static void
ResizeWindow(char **argv) {
       XResizeWindow(dpy, wind(argv[0]), atoi(argv[1]), atoi(argv[2]));
}

static void    
ListWindows(char **argv) {
    Window* kids;
    unsigned i;
    unsigned nkids;

    (void) argv;
    nkids = listwindows(DefaultRootWindow(dpy), &kids);

    for (i = 0; i < nkids; i++) {
        Window* kids2;
        unsigned nkids2;
	unsigned i2;

        nkids2 = listwindows(kids[i], &kids2);

        for (i2 = 0; i2 < nkids2; i2++) {
            XWindowAttributes attr;

            XGetWindowAttributes(dpy, kids2[i2], &attr);
            if(attr.override_redirect == 0 && attr.map_state == IsViewable)
                printwindowname(kids2[i2]);
        }

        if(kids2)
            XFree(kids2);
    }

    if(kids)
        XFree(kids);
}

static void
getWindowProperty(Window window, char * name, Bool delete) {
    Atom prop;
    Atom realType;
    unsigned long n;
    unsigned long extra;
    int format;
    int status;
    char * value;
    
    prop = XInternAtom(dpy, name, True);
    if (prop == None) {
        fprintf(stderr, "%s: no such property '%s'\n", argv0, name);
        return;
    }
    
    status = XGetWindowProperty(dpy, window, prop, 0L, 512L, delete, AnyPropertyType,
        &realType, &format, &n, &extra, (unsigned char **) &value);
    if (status != Success || value == 0 || *value == 0 || n == 0) {
        fprintf(stderr, "%s: couldn't read property on window %lx\n", argv0, window);
        return;
    }
    
    printf("%s\n", value);
}

static void
GetProperty(char ** argv) {
    getWindowProperty(wind(argv[0]), argv[1], False);
}

static void
SetProperty(char ** argv) {
    Atom prop;
    char * name = argv[1];
    char * value = argv[2];
    
    prop = XInternAtom(dpy, name, True);
    if (prop == None) {
        fprintf(stderr, "%s: no such property '%s'\n", argv0, name);
        return;
    }
    
    XChangeProperty(dpy, wind(argv[0]), prop, XA_STRING, 8,
        PropModeReplace, (unsigned char*) value, strlen(value));
}

static void
WarpPointer(char ** argv) {
    XWarpPointer(dpy, None, wind(argv[0]), 0, 0, 0, 0, atoi(argv[1]), atoi(argv[2]));
}

static void
GetFocusWindow(char ** argv) {
    Window focusReturn;
    int revertToReturn;
    (void) argv;
    XGetInputFocus(dpy, &focusReturn, &revertToReturn);
    printwindowname(focusReturn);
}

static void
GetSelection(char ** argv) {
    Window window;
    Atom dataProperty;
    XEvent ev;
    
    (void) argv;

    /* Create an unmapped invisible window. */
    window = XCreateWindow(dpy, DefaultRootWindow(dpy), 0, 0, 1, 1, 0,
        CopyFromParent, InputOnly, CopyFromParent, 0, 0);
    
    /* Ask that the current selection be placed in a property on the window. */
    dataProperty = XInternAtom(dpy, "SELECTION", False);
    XConvertSelection(dpy, XA_PRIMARY, XA_STRING, dataProperty,
        window, CurrentTime);
    
    /* Wait for it to arrive. */
    do {
        XNextEvent(dpy, &ev);
    } while (ev.type != SelectionNotify);
    
    getWindowProperty(window, "SELECTION", True);
}

static void
DeleteSelection(char ** argv) {
    (void) argv;
    XSetSelectionOwner(dpy, XA_PRIMARY, None, CurrentTime);
}

static void
CirculateUp(char ** argv) {
    (void) argv;
    XCirculateSubwindowsUp(dpy, DefaultRootWindow(dpy));
}

static void
CirculateDown(char ** argv) {
    (void) argv;
    XCirculateSubwindowsDown(dpy, DefaultRootWindow(dpy));
}

static ReasonDesc reasons[] = 
{
    { "-move",     3,     MoveWindow,        "<id> <x> <y>" },
    { "-resize",     3,     ResizeWindow,        "<id> <x> <y>" },
    { "-raise",         1,     RaiseWindow,        "<id>" },
    { "-label",        2,    LabelWindow,        "<id> <text>" },
    { "-lower",     1,     LowerWindow,        "<id>" },
    { "-kill",         1,     KillWindow,        "<id>" },
    { "-hide",         1,     HideWindow,        "<id>" },
    { "-unhide",     1,     UnhideWindow,    "<id>" },
    { "-where",     1,     WhereWindow,        "<id>" },
    { "-list",         0,     ListWindows,        "" },
    { "-getprop",    2,    GetProperty,        "<id> <name>" },
    { "-setprop",    3,    SetProperty,        "<id> <name> <value>" },
    { "-warppointer", 3,    WarpPointer,        "<id> <x> <y>" },
    { "-getfocuswindow", 0,    GetFocusWindow,    "" },
    { "-getsel",    0,    GetSelection,        "" },
    { "-delsel",    0,    DeleteSelection,    "" },
    { "-circup",    0,    CirculateUp,        "" },
    { "-circdown",    0,    CirculateDown,        "" },
};

static int
handler(Display *disp, XErrorEvent *err) {
    (void) disp;
    fprintf(stderr, "%s: no window with id %#x\n", argv0, (int) err->resourceid);
    exit(EXIT_FAILURE);
}

static void
usage() {
    ReasonDesc * p;

    fprintf(stderr, "%s: usage:\n", argv0);
    for (p = reasons; p < reasons + sizeof reasons/sizeof reasons[0]; p++) {
        fprintf(stderr, "%s %s %s\n", argv0, p->name, p->usage);
    }
}

extern int
main(int argc, char * argv[]) {
    ReasonDesc * p;
    
    argv0 = argv[0];
    
    dpy = XOpenDisplay("");
    if (dpy == 0) {
        fprintf(stderr, "%s: can't open display.\n", argv0);
        exit(EXIT_FAILURE);
    }
    
    XSetErrorHandler(handler);
    
    if (argc > 1) {
        for (p = reasons; p < reasons + sizeof reasons / sizeof reasons[0]; p++) {
            if (strcmp(p->name, argv[1]) == 0) {
                if (argc - 2 != p->nargs) {
                    fprintf(stderr, "%s: the %s option requires %d argument%s.\n",
                        argv0, argv[1], p->nargs, p->nargs > 1 ? "s" : "");
                    exit(EXIT_FAILURE);
                }
                p->fn(argv + 2);
                XSync(dpy, True);
                return EXIT_SUCCESS;
            }
        }
    }
    
    usage();
    return EXIT_FAILURE;
}
