#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <string.h>

#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>

#include <X11/X.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/keysym.h>

typedef struct HotKey HotKey;
struct HotKey {
    KeySym keysym;
    unsigned int modifiers;
    char* command;
    HotKey* next;
};

/* The list of hot keys. */
HotKey* hotkeys = NULL;

/* The connection to the X server. */
Display* dpy;

/* The number of screens. */
int screen_count;

char* argv0;

const char* attempting_to_grab = 0;
int report_key_grab_error(Display* d, XErrorEvent* e) {
    const char* reason = "unknown reason";
    if (e->error_code == BadAccess) {
        reason = "the key/button combination is already in use by another client";
    } else if (e->error_code == BadValue) {
        reason = "the key code was out of range for XGrabKey";
    } else if (e->error_code == BadWindow) {
        reason = "the root window we passed to XGrabKey was incorrect";
    }
    (void) d;
    fprintf(stderr, "%s: couldn't grab key \"%s\": %s (X error code %i)\n", argv0, attempting_to_grab, reason, e->error_code);
    return 0;
}

void
reap_zombies()
{
    int status;
    while (waitpid(-1, &status, WNOHANG) > 0);
}

void
sigchld_handler()
{
    reap_zombies();
    /* Reinstall this signal handler ready for the next child. */
    signal(SIGCHLD, sigchld_handler);
}

void
shell(const char* command)
{
    char* sh = getenv("SHELL");
    if (sh == 0) {
        sh = "/bin/sh";
    }
    
    switch (fork()) {
    case 0:        /* Child. */
        close(ConnectionNumber(dpy));
        execl(sh, sh, "-c", command, (char*) NULL);
        fprintf(stderr, "%s: can't exec \"%s -c %s\"\n", argv0, sh, command);
        exit(EXIT_FAILURE);
    case -1:    /* Error. */
        fprintf(stderr, "%s: couldn't fork\n", argv0);
        break;
    }
}

void
panic(const char* s)
{
    fprintf(stderr, "%s: %s\n", argv0, s);
    exit(EXIT_FAILURE);
}

unsigned int
parse_modifiers(char* name, const char* full_spec)
{
    char* separator = strchr(name, '-');
    unsigned int modifiers = 0;
    if (separator != NULL) {
        *separator = 0;
        modifiers |= parse_modifiers(separator+1, full_spec);
    }
    if (!strcmp(name, "Shift")) {
        modifiers |= ShiftMask;
    } else if (!strcmp(name, "Control")) {
        modifiers |= ControlMask;
    } else if (!strcmp(name, "Alt") || !strcmp(name, "Mod1")) {
        modifiers |= Mod1Mask;
    } else if (!strcmp(name, "Mod2")) {
        modifiers |= Mod2Mask;
    } else if (!strcmp(name, "Mod3")) {
        modifiers |= Mod3Mask;
    } else if (!strcmp(name, "Super") || !strcmp(name, "Mod4")) {
        modifiers |= Mod4Mask;
    } else {
        fprintf(stderr, "%s: ignoring unknown modifier \"%s\" in \"%s\"\n", argv0, name, full_spec);
    }
    return modifiers;
}

void
add_hot_key(const char* keyname, const char* command)
{
    char* copy = strdup(keyname);
    int screen;
    HotKey* new_key;
    char* unmodified;
    unsigned int modifiers = 0;
    
    unmodified = strrchr(copy, '-');
    if (unmodified == NULL) {
        unmodified = copy;
    } else {
        *unmodified = 0;
        ++ unmodified;
        modifiers = parse_modifiers(copy, keyname);
    }
    
    new_key = (HotKey*) malloc(sizeof(HotKey));
    new_key->keysym = XStringToKeysym(unmodified);
    new_key->modifiers = modifiers;
    new_key->command = strdup(command);
    new_key->next = hotkeys;
    hotkeys = new_key;
    
    XSynchronize(dpy, True);
    attempting_to_grab = keyname;
    XSetErrorHandler(report_key_grab_error);
    for (screen = 0; screen < screen_count; ++screen) {
        Window root = RootWindow(dpy, screen);
        XGrabKey(dpy, XKeysymToKeycode(dpy, new_key->keysym), modifiers, root, False, GrabModeAsync, GrabModeAsync);
    }
    XSetErrorHandler(NULL);
    
    free(copy);
}

void
read_hot_key_file(const char* fname)
{
    FILE* fp;
    char buf[BUFSIZ];
    
    fp = fopen(fname, "r");
    if (fp == NULL) {
        fprintf(stderr, "%s: couldn't read \"%s\"\n", argv0, fname);
        return;
    }
    
    while (fgets(buf, BUFSIZ, fp) != NULL) {
        char* tab = strchr(buf, '\t');
        if (*buf == '#' || tab == NULL) {
            continue;
        }
        *tab = 0;
        add_hot_key(buf, tab+1);
    }
    
    fclose(fp);
}

void
keypress(XEvent* ev0)
{
    XKeyEvent* ev = (XKeyEvent*) ev0;
    KeySym keysym = XKeycodeToKeysym(dpy, (KeyCode)ev->keycode, 0);
    
    HotKey* h = hotkeys;
    for (; h != NULL; h = h->next) {
        if (h->keysym == keysym && ev->state == h->modifiers) {
            shell(h->command);
            break;
        }
    }
}

int
main(int argc, char* argv[])
{
    XEvent ev;
    
    argv0 = argv[0];
    
    /* Open a connection to the X server. */
    dpy = XOpenDisplay("");
    if (dpy == 0) {
        panic("can't open display.");
    }
    
    /* Set up signal handlers. */
#if 0
    signal(SIGTERM, SIG_IGN);
    signal(SIGINT, SIG_IGN);
#endif
    signal(SIGHUP, SIG_IGN);
    signal(SIGCHLD, sigchld_handler);
    
    screen_count = ScreenCount(dpy);
    
    if (argc != 2) {
        panic("syntax: speckeysd <keys file>");
    }
    
    read_hot_key_file(argv[1]);
    
    /* Make sure all our communication to the server got through. */
    XSync(dpy, False);
    
    /* The main event loop. */
    for (;;) {
        XNextEvent(dpy, &ev);
        switch (ev.type) {
        case KeyPress:
            keypress(&ev);
            break;
        case MappingNotify:
            XRefreshKeyboardMapping((XMappingEvent*) &ev);
            break;
        default:
            /* Do I look like I care? */
            break;
        }
    }
}
