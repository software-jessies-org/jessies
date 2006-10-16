#include    <stdio.h>
#include    <stdlib.h>
#include    <signal.h>

#include    <X11/Xlib.h>

#ifdef linux
#include <fcntl.h>
#include <sys/vt.h>
#endif

#include    "lock.h"

Display * dpy;            /* The connection to the X server. */
int screen_count;        /* The number of screens. */

ScreenInfo * screens;

XFontStruct * font;        /* Font. */

char * argv0;

static void initScreens(void);
static void initScreen(int);

/*ARGSUSED*/
int
main(int argc, char *argv[]) {
    XEvent ev;
    
    argv0 = argv[0];
    
    /* Open a connection to the X server. */
    dpy = XOpenDisplay("");
    if (dpy == 0)
        Panic("can't open display.");
    
    get_resources();
    
    /* Set up an error handler. */
    XSetErrorHandler(ErrorHandler);
    
    /* Set up signal handlers. */
    signal(SIGTERM, SIG_IGN);
    signal(SIGINT, SIG_IGN);
    signal(SIGHUP, SIG_IGN);
    
    /* Get font. */
    font = XLoadQueryFont(dpy, font_name);
    if (font == 0)
        font = XLoadQueryFont(dpy, "fixed");
    if (font == 0)
        Panic("can't find a font.");
    
    initScreens();
    
    /* Grab the keyboard. */
    XGrabKeyboard(dpy, screens[0].window, False,
        GrabModeAsync, GrabModeAsync, CurrentTime);
#ifdef linux
    {
        int fd = open("/dev/console", O_RDWR);
        if (fd != -1) {
            ioctl(fd, VT_LOCKSWITCH, 0);
//            close(fd);
        }
        fprintf(stderr, "/dev/console = %i\n", fd);
    }
#endif
    
    /* Make sure all our communication to the server got through. */
    XSync(dpy, False);
    
    /* The main event loop. */
    for (;;) {
        XNextEvent(dpy, &ev);
        dispatch(&ev);
    }
}

static void
initScreens(void) {
    int screen;
    
    /* Find out how many screens we've got, and allocate space for their info. */
    screen_count = ScreenCount(dpy);
    screens = (ScreenInfo *) malloc(sizeof(ScreenInfo) * screen_count);
    
    /* Go through the screens one-by-one, initialising them. */
    for (screen = 0; screen < screen_count; screen++)
        initScreen(screen);
}

static void
initScreen(int screen) {
    XGCValues gv;
    XSetWindowAttributes attr;
    Cursor no_cursor;
    XColor colour;
    
    /* Find the screen's dimensions. */
    screens[screen].width = DisplayWidth(dpy, screen);
    screens[screen].height = DisplayHeight(dpy, screen);

    /* Get the pixel values of the only two colours we use. */
    screens[screen].black = BlackPixel(dpy, screen);
    screens[screen].white = WhitePixel(dpy, screen);

    /* Create the locking window. */
    attr.override_redirect = True;
    attr.background_pixel = screens[screen].black;
    attr.border_pixel = screens[screen].black;
    attr.event_mask = ExposureMask | VisibilityChangeMask |
        StructureNotifyMask;
    attr.do_not_propagate_mask = ButtonPressMask | ButtonReleaseMask;
    screens[screen].window = XCreateWindow(dpy,
        RootWindow(dpy, screen), 0, 0,
        screens[screen].width, screens[screen].height, 0,
        CopyFromParent, InputOutput, CopyFromParent,
        CWOverrideRedirect | CWBackPixel | CWBorderPixel | CWEventMask |
        CWDontPropagate, &attr);

    XStoreName(dpy, screens[screen].window, "lock");

    /* Create GC. */
    gv.foreground = screens[screen].white;
    gv.background = screens[screen].black;
    gv.font = font->fid;
    screens[screen].gc = XCreateGC(dpy, screens[screen].window,
        GCForeground | GCBackground | GCFont, &gv);

    /* Hide the cursor. */
    no_cursor = XCreateGlyphCursor(dpy, font->fid, font->fid, ' ', ' ',
        &colour, &colour);
    XDefineCursor(dpy, screens[screen].window, no_cursor);

    /* Bring up the lock window. */
    XMapRaised(dpy, screens[screen].window);
}

ScreenInfo *
getScreenForWindow(Window w) {
    int screen;
    for (screen = 0; screen < screen_count; screen++)
        if (screens[screen].window == w)
            return &screens[screen];
    return 0;
}
