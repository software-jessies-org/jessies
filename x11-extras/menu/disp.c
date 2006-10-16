#include "menu.h"

char time_string[17]; /* 1234-67-90 23:56 */

/* Dispatcher for main event loop. */
typedef struct Disp Disp;
struct Disp {
    int    type;
    void    (*handler)(XEvent *);
};

static void updateTime();
static void execute(MenuItem *);

static void nullEvent(XEvent *);
static void buttonPress(XEvent *);
static void buttonRelease(XEvent *);
static void mouseMoved(XEvent *);
static void expose(XEvent *);
static void enter(XEvent *);
static void leave(XEvent *);
static void mappingnotify(XEvent *);
static void visibilitynotify(XEvent *);

static Disp disps[] = {
    {NullEvent, nullEvent},
    {ButtonPress, buttonPress},
    {ButtonRelease, buttonRelease},
    {MotionNotify, mouseMoved},
    {Expose, expose},
    {EnterNotify, enter},
    {LeaveNotify, leave},
    {MappingNotify, mappingnotify},
    {VisibilityNotify, visibilitynotify},
};

extern void
dispatch(XEvent * ev) {
    Disp * p;
    
    for (p = disps; p < disps + sizeof disps / sizeof disps[0]; p++) {
        if (p->type == ev->type) {
            if (p->handler != 0)
                p->handler(ev);
            return;
        }
    }
}

static void
updateTime() {
    time_t t;
    struct tm tm;
    char * fmt;
    
    time(&t);
    tm = *localtime(&t);
    fmt = (tm.tm_sec & 1) ? "%Y-%m-%d %H:%M" : "%Y-%m-%d %H.%M";
    strftime(time_string, sizeof time_string, fmt, &tm);
}

static void
expose(XEvent * ev) {
    int width;
    int x;
    MenuItem * item;
    
    /* Only handle the last in a group of Expose events. */
    if (ev && ev->xexpose.count != 0)
        return;
    
    /* Clear the window. */
    XClearWindow(dpy, window);
    
    /* Shave off the corners. */
    XFillRectangle(dpy, window, gc, 0, 0, 10, 10);
    XFillRectangle(dpy, window, gc, display_width-10, 0, 10, 10);
    XSetForeground(dpy, gc, white);
    XFillArc(dpy, window, gc, 0, 0, 18, 18, 0, 360*64);
    XFillArc(dpy, window, gc, display_width-18, 0, 18, 18, 0, 360*64);
    XSetForeground(dpy, gc, black);
    
    /* Draw the menu. */
    x = 20;
    for (item = menu; item != 0; item = item->next) {
        int width = XTextWidth(font, item->name, strlen(item->name));
        XDrawString(dpy, window, gc, x, 1.1 * font->ascent,
            item->name, strlen(item->name));
        if (item == selected) {
            XSetFunction(dpy, gc, GXinvert);
            XFillRectangle(dpy, window, gc, x - 5, 0, width + 10, 20);
            XSetFunction(dpy, gc, GXcopy);
        }
        x += 20 + width;
    }
    
    /* Draw the clock. */
    updateTime();
    width = XTextWidth(font, time_string, strlen(time_string));
    XDrawString(dpy, window, gc, display_width - width - 20,
        1.2 * font->ascent,
        time_string, strlen(time_string));
}

static void
nullEvent(XEvent * ev) {
    (void) ev;
    /* Ensure that the clock is redrawn. */
    expose(0);
}

static MenuItem *
whichItem(int mouseX) {
    int x = 0;
    MenuItem * item;
    
    for (item = menu; item != 0; item = item->next) {
        int itemWidth = 20 + XTextWidth(font, item->name,
            strlen(item->name));
        if (mouseX >= x && mouseX <= (x + itemWidth)) {
            return item;
        }
        x += itemWidth;
    }
    
    return 0;
}

static void
mouseMoved(XEvent * ev) {
    while (XCheckMaskEvent(dpy, ButtonMotionMask, ev))
        ;
    
    if (!XQueryPointer(dpy, ev->xmotion.window, &(ev->xmotion.root),
        &(ev->xmotion.subwindow), &(ev->xmotion.x_root),
        &(ev->xmotion.y_root), &(ev->xmotion.x), &(ev->xmotion.y),
        &(ev->xmotion.state)))
            return;
    
    selected = whichItem(ev->xmotion.x);
    expose(0);
}

static void
buttonPress(XEvent * ev) {
    selected = whichItem(ev->xbutton.x);
    expose(0);
}

static void
buttonRelease(XEvent * ev) {
    (void) ev;
    if (selected != 0) {
        execute(selected);
        selected = 0;
    }
    expose(0);
}

static void
mappingnotify(XEvent * ev) {
    XRefreshKeyboardMapping((XMappingEvent *) ev);
}

static void
enter(XEvent * ev) {
    (void) ev;
}

static void
leave(XEvent * ev) {
    (void) ev;
    selected = 0;
    expose(0);
}

static void
execute(MenuItem * item) {
    static char * sh;
    
    if (item->command == 0)
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
            execl(sh, sh, "-c", item->command, (char*) NULL);
            fprintf(stderr, "%s: can't exec \"%s -c %s\"\n", argv0, sh,
                item->command);
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

static void
visibilitynotify(XEvent * ev) {
    (void) ev;
}
