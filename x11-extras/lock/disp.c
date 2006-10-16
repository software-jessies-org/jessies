#include    <X11/X.h>
#include    <X11/Xlib.h>
#include    <X11/Xutil.h>
#include    <X11/keysym.h>

#include    <ctype.h>
#include    <stdio.h>
#include    <stdlib.h>
#include    <string.h>

#ifdef LINUX
#include <fcntl.h>
#include <sys/vt.h>
#endif

#include    "lock.h"

/* Dispatcher for main event loop. */
typedef struct Disp Disp;
struct Disp {
    int    type;
    void    (*handler)(XEvent *);
};

static void expose(XEvent *);
static void exposeScreen(ScreenInfo *);
static void keypress(XEvent *);
static void mappingnotify(XEvent *);
static void raiseLockWindows(void);

static Disp disps[] =
{
    {Expose, expose},
    {KeyPress, keypress},
    {KeyRelease, 0},
    {MappingNotify, mappingnotify},
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

    raiseLockWindows();
}

static char password[14];
static char stars[14] = "*************";
static int chars = 0;

static void
expose(XEvent * ev) {
    /* Only handle the last in a group of Expose events. */
    if (ev->xexpose.count != 0)
        return;
    
    exposeScreen(getScreenForWindow(ev->xexpose.window));
}

static void
exposeScreen(ScreenInfo * screen) {
    int len;
    int width;
    
    if (screen != &screens[0])
        return;
    
    /* Do the redraw. */
    len = strlen(lock_string);
    width = XTextWidth(font, lock_string, len);
    XDrawString(dpy, screen->window, screen->gc,
        (screen->width - width)/2, screen->height/4,
        lock_string, len);

    width = XTextWidth(font, stars, chars);
    XSetForeground(dpy, screen->gc, screen->black);
    XFillRectangle(dpy, screen->window, screen->gc,
        0, screen->height/2 - 100, screen->width, 100);
    XSetForeground(dpy, screen->gc, screen->white);
    XDrawString(dpy, screen->window, screen->gc,
        (screen->width - width)/2, screen->height/2,
        stars, chars);
}

static void
keypress(XEvent * ev) {
    char buffer[1];
    KeySym keysym;
    int c;
    
    c = XLookupString((XKeyEvent *) ev, buffer, sizeof buffer, &keysym, 0);
    
    if (keysym == XK_Return) {
        password[chars] = 0;
        if (check_password(password)) {
            XUngrabKeyboard(dpy, CurrentTime);
#ifdef LINUX
            {
                int fd = open("/dev/console", O_RDWR);
                if (fd != -1)
                    ioctl(fd, VT_UNLOCKSWITCH, 0);
            }
#endif
            exit(EXIT_SUCCESS);
        } else {
            XBell(dpy, 100);
            chars = 0;
        }
    } else if (keysym == XK_BackSpace) {
        if (chars > 0)
            chars--;
    } else if (keysym == XK_Escape)
        chars = 0;
    else if (isprint(*buffer) && chars + 1 <= 13)
        password[chars++] = *buffer;
    
    exposeScreen(&screens[0]);
}

static void
mappingnotify(XEvent * ev) {
    XRefreshKeyboardMapping((XMappingEvent *) ev);
}

static void
raiseLockWindows(void) {
    int screen;
    for (screen = 0; screen < screen_count; screen++)
        XRaiseWindow(dpy, screens[screen].window);
}
