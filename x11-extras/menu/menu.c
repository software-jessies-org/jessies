#include    "menu.h"

Display * dpy;            /* The connection to the X server. */
int display_width;        /* The width of the screen we're managing. */
int display_height;        /* The height of the screen we're managing. */

Window window;        /* Main window. */
Window root;            /* Root window. */
GC gc;                /* The default GC. */

unsigned long black;        /* Black pixel. */
unsigned long white;    /* White pixel. */

XFontStruct * font;        /* Font. */
Cursor mouse_cursor;    /* Mouse cursor. */

MenuItem * menu = 0;
MenuItem * selected = 0;

static int forceRestart;
char * argv0;

static void readMenu();
static void addMenuItem(char *, char *);
static void getEvent(XEvent *);
static void initCursor(void);

static void restartSelf(int signum) {
    forceRestart = 1;
}

/*ARGSUSED*/
int
main(int argc, char *argv[]) {
    XEvent ev;
    XGCValues gv;
    XSetWindowAttributes attr;
    
    (void) argc;
    argv0 = argv[0];
    struct sigaction sa;
    sa.sa_handler = restartSelf;
    sigemptyset(&(sa.sa_mask));
    sigaddset(&(sa.sa_mask), SIGHUP);
    if (sigaction(SIGHUP, &sa, NULL)) {
        fprintf(stderr, "Failed to set up SIGHUP handler.\n");
    }
    
    /* Open a connection to the X server. */
    dpy = XOpenDisplay("");
    if (dpy == 0)
        Panic("can't open display.");
    
    get_resources();
    
    /* Find the screen's dimensions. */
    display_width = DisplayWidth(dpy, DefaultScreen(dpy));
    display_height = DisplayHeight(dpy, DefaultScreen(dpy));
    
    /* Set up an error handler. */
    XSetErrorHandler(ErrorHandler);
    
    /* Get the pixel values of the only two colours we use. */
    black = BlackPixel(dpy, DefaultScreen(dpy));
    white = WhitePixel(dpy, DefaultScreen(dpy));
    
    /* Get font. */
    font = XLoadQueryFont(dpy, font_name);
    if (font == 0)
        font = XLoadQueryFont(dpy, "fixed");
    if (font == 0)
        Panic("can't find a font.");
    
    /* Get a cursor. */
    initCursor();
    
    /* Create the window. */
    root = DefaultRootWindow(dpy);
    attr.override_redirect = True;
    attr.background_pixel = white;
    attr.border_pixel = black;
    attr.cursor = mouse_cursor;
    attr.event_mask = ExposureMask | VisibilityChangeMask |
        ButtonMotionMask | PointerMotionHintMask |
        ButtonPressMask | ButtonReleaseMask | StructureNotifyMask |
        EnterWindowMask | LeaveWindowMask;
    window = XCreateWindow(dpy, root,
        0, 0,
        display_width, 1.2 * (font->ascent + font->descent),
        0, CopyFromParent, InputOutput, CopyFromParent,
        CWOverrideRedirect | CWBackPixel | CWBorderPixel |
        CWCursor | CWEventMask,
        &attr);
    
    /* Create GC. */
    gv.foreground = black;
    gv.background = white;
    gv.font = font->fid;
    gc = XCreateGC(dpy, window, GCForeground | GCBackground | GCFont,
        &gv);
    
    /* Create the menu items. */
    readMenu();
    
    /* Bring up the window. */
    XMapRaised(dpy, window);
    
    /* Make sure all our communication to the server got through. */
    XSync(dpy, False);
    
    /* The main event loop. */
    while (!forceRestart) {
        getEvent(&ev);
        dispatch(&ev);
    }
    // Someone hit us with a SIGHUP: better exec ourselves to force a config
    // reload and cope with changing screen sizes.
    execvp(argv0, argv);
}

static void getEvent(XEvent * ev) {
    int fd;
    fd_set readfds;
    struct timeval tv;
    
    /* Is there a message waiting? */
    if (QLength(dpy) > 0) {
        XNextEvent(dpy, ev);
        return;
    }
    
    /* Beg... */
    XFlush(dpy);
    
    /* Wait one second to see if a message arrives. */
    fd = ConnectionNumber(dpy);
    FD_ZERO(&readfds);
    FD_SET(fd, &readfds);
    tv.tv_sec = 1;
    tv.tv_usec = 0;
    if (select(fd + 1, &readfds, 0, 0, &tv) == 1) {
        XNextEvent(dpy, ev);
        return;
    }
    
    /* No message, so we have a null event. */
    ev->type = NullEvent;
}

static void
initCursor() {
    XColor red, white, exact;
    Colormap cmp = DefaultColormap(dpy, DefaultScreen(dpy));
    XAllocNamedColor(dpy, cmp, "red", &red, &exact);
    XAllocNamedColor(dpy, cmp, "white", &white, &exact);
    mouse_cursor = XCreateFontCursor(dpy, XC_left_ptr);
    XRecolorCursor(dpy, mouse_cursor, &red, &white);
}

static void
addMenuItem(char * name, char * command) {
    MenuItem * newItem = (MenuItem *) malloc(sizeof(MenuItem));
    if (newItem == 0)
        return;
    
    newItem->next = 0;
    newItem->name = strdup(name);
    newItem->command = strdup(command);
    
    if (menu == 0) {
        menu = newItem;
    } else {
        MenuItem * last = menu;
        while (last->next != 0)
            last = last->next;
        last->next = newItem;
    }
}

static void
readMenu() {
    FILE * fp;
    char line[BUFSIZ];
    
    fp = fopen(".menu", "r");
    if (fp == 0) {
        addMenuItem("localhost", "exec xterm");
        return;
    }
    
    while (fgets(line, BUFSIZ, fp) != 0) {
        char * tab = strchr(line, '\t');
        if (*line == '#' || tab == 0)
            continue;
        *tab = 0;
        addMenuItem(line, tab+1);
    }
    
    fclose(fp);
}
