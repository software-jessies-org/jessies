#include "clock.h"

int sinister;            /* Live on the left rather than the right? */
char * view_command;    /* User's view command. */
char * font_name;        /* User's font. */
char * command[6];        /* User's button commands (element 0 unused). */

char *
sdup(char *p) {
    char    *s ;

    s = malloc(strlen(p) + 1);
    if(s == 0)
        Panic("malloc fails");
    return strcpy(s, p);
}

extern void
get_resources(void) {
    XrmDatabase db;
    XrmValue value;
    char * resource_manager;
    char * type;
    int i;

    /* Set our fall-back defaults. */
    font_name = DEFAULT_FONT;
    for (i = 0; i < 6; i++)
        command[i] = 0;
    view_command = 0;
    sinister = 0;
    
    resource_manager = XResourceManagerString(dpy);
    if (resource_manager == 0)
        return;

    XrmInitialize();
    db = XrmGetStringDatabase(resource_manager);
    if (db == 0)
        return;

    /* Font. */
    if (XrmGetResource(db, "clock.font", "Font", &type, &value) == True)
        if (strcmp(type, "String") == 0)
            font_name = sdup((char *) value.addr);

    /* Button commands. */
    for (i = 1; i < 6; i++) {
        char resource[15];
        sprintf(resource, "clock.button%i", i);
        if (XrmGetResource(db, resource, "String", &type, &value) == True)
            if (strcmp(type, "String") == 0)
                command[i] = sdup((char *) value.addr);
    }
    
    /* View command. */
    if (XrmGetResource(db, "clock.viewCommand", "String", &type, &value) == True)
        if (strcmp(type, "String") == 0)
            view_command = sdup((char *) value.addr);
    
    /* Leftist tendency? */
    if (XrmGetResource(db, "clock.leftHanded", "Existential", &type, &value) == True)
        sinister = 1;
}
