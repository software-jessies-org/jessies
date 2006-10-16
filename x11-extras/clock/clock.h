#define DEFAULT_FONT    "lucidasans-bold-14"

#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#include <unistd.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/wait.h>

#include <X11/X.h>
#include <X11/Xos.h>
#include <X11/Xlib.h>
#include <X11/Xresource.h>
#include <X11/Xutil.h>
#include <X11/Xproto.h>
#include <X11/Xatom.h>

/*    clock.c    */
extern Display * dpy;
extern int display_width;
extern int display_height;
extern char * display_string;
extern int window_width;
extern int window_height;
extern Window root;
extern Window window;
extern GC gc;
extern unsigned long black;
extern unsigned long white;
extern XFontStruct * font;
extern char * argv0;

/*    disp.c    */
extern void dispatch(XEvent *);
extern void update_clock();

/*    error.c    */
extern int ErrorHandler(Display *, XErrorEvent *);
extern void Panic(char *);

/*    resource.c    */
extern int sinister;
extern char * view_command;
extern char * font_name;
extern char * command[];
char * sdup(char *);
extern void get_resources();
