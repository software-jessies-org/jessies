#include    <stdio.h>
#include    <stdlib.h>

#include    <unistd.h>
#include    <sys/types.h>
#include    <sys/wait.h>

#include    <X11/X.h>
#include    <X11/Xos.h>
#include    <X11/Xlib.h>
#include    <X11/Xresource.h>
#include    <X11/Xutil.h>
#include    <X11/Xatom.h>

#include    "lock.h"


char    * font_name;        /* User's font. */
char    * lock_string;        /* User's string for "locked" message. */

char *
sdup(char *p)
{
    char    *s ;

    s = malloc(strlen(p) + 1);
    if(s == 0)
        Panic("malloc fails");
    return strcpy(s, p);
}

extern void
get_resources(void)
{
    XrmDatabase    db;
    XrmValue    value;
    char    *resource_manager;
    char    *type;

    /* Set our fall-back defaults. */
    font_name = DEFAULT_FONT;
    lock_string = DEFAULT_LOCK_STRING;

    resource_manager = XResourceManagerString(dpy);
    if (resource_manager == 0)
        return;

    XrmInitialize();
    db = XrmGetStringDatabase(resource_manager);
    if (db == 0)
        return;

    /* Font. */
    if (XrmGetResource(db, "lock.font", "Font", &type, &value) == True)
        if (strcmp(type, "String") == 0)
            font_name = sdup((char *) value.addr);

    /* Lock string. */
    if (XrmGetResource(db, "lock.message", "String", &type, &value) == True)
        if (strcmp(type, "String") == 0)
            lock_string = sdup((char *) value.addr);
}
