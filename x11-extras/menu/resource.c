#include "menu.h"

/* Command which renders the text to show instead of the clock. This may be
 * enhanced to show information other than a mere clock, for example it could
 * be a script (on a laptop) which displays the battery charge level alongside
 * a clock. The command will be run once per second, and the resulting text
 * shown in the menu. */
char * clock_command;
char * font_name;        /* User's font. */
char * command[6];        /* User's button commands (element 0 unused). */

extern void
get_resources(void) {
    XrmDatabase db;
    XrmValue value;
    char * resource_manager;
    char * type;

    /* Set our fall-back defaults. */
    font_name = DEFAULT_FONT;
    clock_command = 0;
    
    resource_manager = XResourceManagerString(dpy);
    if (resource_manager == 0)
        return;

    XrmInitialize();
    db = XrmGetStringDatabase(resource_manager);
    if (db == 0)
        return;

    /* Font. */
    if (XrmGetResource(db, "menu.font", "Font", &type, &value) == True)
        if (strcmp(type, "String") == 0)
            font_name = strdup((char *) value.addr);

    /* Command to run to show the time/status on the right side of the menu. */
    if (XrmGetResource(db, "menu.clockCommand", "String", &type, &value) == True) {
        if (strcmp(type, "String") == 0) {
            clock_command = strdup((char *) value.addr);
        }
    }
}
