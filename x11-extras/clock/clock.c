#include    "clock.h"

Display * dpy;            /* The connection to the X server. */
int display_width;        /* The width of the screen we're managing. */
int display_height;        /* The height of the screen we're managing. */
int window_width;        /* The width of the window. */
int window_height;        /* The height of the window. */

Window window;        /* Main window. */
Window root;            /* Root window. */
GC gc;                /* The default GC. */

unsigned long black;        /* Black pixel. */
unsigned long white;    /* White pixel. */

XFontStruct * font;        /* Font. */

char * argv0;

/*ARGSUSED*/
int
main(int argc, char *argv[]) {
    XEvent ev;
    XGCValues gv;
    XSetWindowAttributes attr;
    
    (void) argc;
    argv0 = argv[0];
    
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
    
    window_width = 100; /* Arbitrary: ephemeral. */
    window_height = 2 * (font->ascent + font->descent);
    
    /* Create the window. */
    root = DefaultRootWindow(dpy);
    attr.override_redirect = True;
    attr.background_pixel = black;
    attr.border_pixel = black;
    attr.event_mask = ExposureMask | VisibilityChangeMask |
        ButtonPressMask | ButtonReleaseMask | StructureNotifyMask |
        EnterWindowMask | LeaveWindowMask;
    window = XCreateWindow(dpy, root,
        sinister ? 0 : display_width-2, 0,
        2, 2,
        0, CopyFromParent, InputOutput, CopyFromParent,
        CWOverrideRedirect | CWBackPixel | CWBorderPixel | CWEventMask,
        &attr);
    
    /* Create GC. */
    gv.foreground = white;
    gv.background = black;
    gv.font = font->fid;
    gc = XCreateGC(dpy, window, GCForeground | GCBackground | GCFont,
        &gv);
    
    /* Bring up the lock window. */
    XMapRaised(dpy, window);
    
    /* Make sure all our communication to the server got through. */
    XSync(dpy, False);
    
    /* The main event loop. */
    for (;;) {
        XNextEvent(dpy, &ev);
        dispatch(&ev);
    }
}
