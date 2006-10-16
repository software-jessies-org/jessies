#include    <stdio.h>
#include    <stdlib.h>

#include    <X11/X.h>
#include    <X11/Xlib.h>
#include    <X11/Xutil.h>
#include    <X11/Xproto.h>

#include    "lock.h"

int ignore_badwindow;

void
Panic(char *s)
{
    fprintf(stderr, "%s: %s\n", argv0, s);
    exit(EXIT_FAILURE);
}

int
ErrorHandler(Display *d, XErrorEvent *e)
{
    char    msg[80];
    char    req[80];
    char    number[80];

    if (ignore_badwindow &&
        (e->error_code == BadWindow || e->error_code == BadColor))
            return 0;

    XGetErrorText(d, e->error_code, msg, sizeof(msg));
    sprintf(number, "%d", e->request_code);
    XGetErrorDatabaseText(d, "XRequest", number, number, req, sizeof(req));

    fprintf(stderr, "%s: protocol request %s on resource %#x failed: %s\n",
        argv0, req, (unsigned int) e->resourceid, msg);

    return 0;
}
