#define DEFAULT_FONT    "-*-lucida-bold-r-normal-sans-*-250-*-*-*-*-*-*"
#define DEFAULT_LOCK_STRING    "This X display is locked. Please supply the password."

typedef struct ScreenInfo ScreenInfo;
struct ScreenInfo {
    Window window;
    GC gc;
    
    /* Screen dimensions. */
    int width, height;
    
    /* Pixel values. */
    unsigned long black, white;
};

/*    lock.c    */
extern Display * dpy;
extern XFontStruct * font;
extern char * argv0;
extern int screen_count;
extern ScreenInfo * screens;
extern ScreenInfo * getScreenForWindow(Window);

/*    disp.c    */
extern void dispatch(XEvent *);

/*    error.c    */
extern int ignore_badwindow;
extern int ErrorHandler(Display *, XErrorEvent *);
extern void Panic(char *);

/*    password.c    */
extern int check_password(char *);

/*    resource.c    */
extern char * font_name;
extern char * lock_string;
extern void get_resources();
