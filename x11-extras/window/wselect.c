#include <stdio.h>
#include <stdlib.h>

#include <X11/X.h>
#include <X11/Xos.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/Xatom.h>
#include <X11/cursorfont.h>

#define USED(x)
#define MASK (ButtonPressMask|ButtonReleaseMask)

static char    *argv0;

static void
exits(char *p)
{
    fprintf(stderr, "%s: %s\n", argv0, p);
    exit(EXIT_FAILURE);
}

static Window
get_window_id(Display * dpy, int screen, int button)
{
    Cursor cursor;
    Window root;
    Window retwin = None;
    int retbutton = -1;
    int pressed = 0;
    int idummy;
    unsigned udummy;
    XEvent event;

    root = RootWindow(dpy, screen);

    cursor = XCreateFontCursor(dpy, XC_draped_box);
    if(cursor == None)
        exits("unable to create selection cursor");

    if(XGrabPointer(dpy, root, False, MASK, GrabModeSync, GrabModeAsync, None, cursor, CurrentTime) != GrabSuccess)
        exits("unable to grab cursor");

    while(retwin == None || pressed != 0) {
        XAllowEvents(dpy, SyncPointer, CurrentTime);
        XWindowEvent(dpy, root, MASK, &event);
        switch(event.type) {
        case ButtonPress:
            if(retwin == None) {
                retbutton = event.xbutton.button;
                retwin =((event.xbutton.subwindow != None) ?
                    event.xbutton.subwindow : root);
            }
            pressed++;
            break;
        case ButtonRelease:
            if(pressed > 0)
                pressed--;
            break;
        }
    }

    XUngrabPointer(dpy, CurrentTime);
    XFreeCursor(dpy, cursor);
    XSync(dpy, 0);
    
    /* Find the client window corresponding to the window that was clicked in. */
    if (XGetGeometry(dpy, retwin, &root, &idummy, &idummy, &udummy, &udummy, &udummy, &udummy) && retwin != root)
        retwin = XmuClientWindow(dpy, retwin);

    return (button == -1 || retbutton == button) ? retwin : None;
}

extern int
main(int argc, char **argv)
{
    Display * display;
    int screen;
    Window id;

    USED(argc);
    argv0 = argv[0];

    display = XOpenDisplay("");
    if(display == 0)
        exits("can't open display");

    screen = DefaultScreen(display);

    id = get_window_id(display, screen, 1);
    if(id == None)
        return EXIT_FAILURE;
    
    printf("%#x\n",(int) id);
    return 0;
}
