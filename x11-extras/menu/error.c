#include    "menu.h"

void
Panic(char *s) {
    fprintf(stderr, "%s: %s\n", argv0, s);
    exit(EXIT_FAILURE);
}

int
ErrorHandler(Display *d, XErrorEvent *e) {
    char    msg[80];
    char    req[80];
    char    number[80];

    XGetErrorText(d, e->error_code, msg, sizeof(msg));
    sprintf(number, "%d", e->request_code);
    XGetErrorDatabaseText(d, "XRequest", number, number, req, sizeof(req));

    fprintf(stderr, "%s: protocol request %s on resource %#x failed: %s\n",
        argv0, req, (unsigned int) e->resourceid, msg);

    return 0;
}
