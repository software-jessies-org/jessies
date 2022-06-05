#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <cerrno>

#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#include <X11/X.h>
#include <X11/XKBlib.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/keysym.h>

#include <initializer_list>

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
char* hot_key_file;

const char* attempting_to_grab = 0;

int report_key_grab_error(Display* /*d*/, XErrorEvent* e) {
  const char* reason = "unknown reason";
  if (e->error_code == BadAccess) {
    reason = "the key/button combination is already in use by another client";
  } else if (e->error_code == BadValue) {
    reason = "the key code was out of range for XGrabKey";
  } else if (e->error_code == BadWindow) {
    reason = "the root window we passed to XGrabKey was incorrect";
  }
  fprintf(stderr, "%s: couldn't grab key \"%s\": %s (X error code %i)\n", argv0,
          attempting_to_grab, reason, e->error_code);
  return 0;
}

static void reap_zombies() {
  int status;
  while (waitpid(-1, &status, WNOHANG) > 0)
    ;
}

static void sigchld_handler(int /*signal_number*/) {
  reap_zombies();
  /* Reinstall this signal handler ready for the next child. */
  signal(SIGCHLD, sigchld_handler);
}

static void shell(const char* command) {
  const char* sh = getenv("SHELL");
  if (sh == 0) {
    sh = "/bin/sh";
  }

  switch (fork()) {
    case 0: /* Child. */
      close(ConnectionNumber(dpy));
      execl(sh, sh, "-c", command, (char*)NULL);
      fprintf(stderr, "%s: can't exec \"%s -c %s\"\n", argv0, sh, command);
      exit(EXIT_FAILURE);
    case -1: /* Error. */
      fprintf(stderr, "%s: couldn't fork\n", argv0);
      break;
  }
}

static void panic(const char* s) {
  fprintf(stderr, "%s: %s\n", argv0, s);
  exit(EXIT_FAILURE);
}

unsigned int parse_modifiers(char* name, const char* full_spec) {
  char* separator = strchr(name, '-');
  unsigned int modifiers = 0;
  if (separator != NULL) {
    *separator = 0;
    modifiers |= parse_modifiers(separator + 1, full_spec);
  }
  // We don't interpret modifies Lock (caps lock), Mod2 (num lock) or Mod3
  // (scroll lock), because they're stateful buttons, not real modifiers in the
  // normal hotkey sense.
  if (!strcmp(name, "Shift")) {
    modifiers |= ShiftMask;
  } else if (!strcmp(name, "Control")) {
    modifiers |= ControlMask;
  } else if (!strcmp(name, "Alt") || !strcmp(name, "Mod1")) {
    modifiers |= Mod1Mask;
  } else if (!strcmp(name, "Super") || !strcmp(name, "Mod4")) {
    modifiers |= Mod4Mask;
  } else {
    fprintf(stderr, "%s: ignoring unknown modifier \"%s\" in \"%s\"\n", argv0,
            name, full_spec);
  }
  return modifiers;
}

static void add_hot_key_modified(const char* keyname,
                                 const char* command,
                                 unsigned int base_mod) {
  char* copy = strdup(keyname);
  char* unmodified = strrchr(copy, '-');
  unsigned int modifiers = 0;
  if (unmodified == NULL) {
    unmodified = copy;
  } else {
    *unmodified = 0;
    ++unmodified;
    modifiers = parse_modifiers(copy, keyname);
  }
  modifiers |= base_mod;

  HotKey* new_key = new HotKey;
  new_key->keysym = XStringToKeysym(unmodified);
  new_key->modifiers = modifiers;
  new_key->command = strdup(command);
  new_key->next = hotkeys;
  hotkeys = new_key;

  XSynchronize(dpy, True);
  attempting_to_grab = keyname;
  XSetErrorHandler(report_key_grab_error);
  for (int screen = 0; screen < screen_count; ++screen) {
    Window root = RootWindow(dpy, screen);
    XGrabKey(dpy, XKeysymToKeycode(dpy, new_key->keysym), modifiers, root,
             False, GrabModeAsync, GrabModeAsync);
  }
  XSetErrorHandler(NULL);

  free(copy);
}

static void add_hot_key(const char* keyname, const char* command) {
  // https://stackoverflow.com/questions/4037230/global-hotkey-with-x11-xlib/4037579
  // Xlib is very particular about the mod key mask. Having caps lock, num lock,
  // scroll lock etc enabled or disabled results in a different mod mask, and
  // thus will fail to match if we don't grab that key too.
  // In X11 terms, we want to ignore these:
  // LockMask = caps lock
  // Mod2Mask = num lock
  // Mod3Mask = scroll lock
  for (int capsMask : {0, LockMask}) {
    for (int numLockMask : {0, Mod2Mask}) {
      for (int scrLockMask : {0, Mod3Mask}) {
        auto combinedMask = capsMask | numLockMask | scrLockMask;
        add_hot_key_modified(keyname, command, combinedMask);
      }
    }
  }
}

static void read_hot_key_file(const char* fname) {
  FILE* fp = fopen(fname, "r");
  if (fp == NULL) {
    fprintf(stderr, "%s: couldn't read \"%s\"\n", argv0, fname);
    return;
  }

  char buf[BUFSIZ];
  while (fgets(buf, BUFSIZ, fp) != NULL) {
    char* tab = strchr(buf, '\t');
    if (*buf == '#' || tab == NULL) {
      continue;
    }
    *tab = 0;
    add_hot_key(buf, tab + 1);
  }

  fclose(fp);
}

static void keypress(XEvent* ev) {
  KeySym keysym = XkbKeycodeToKeysym(dpy, ev->xkey.keycode, 0,
                                     ev->xkey.state & ShiftMask ? 1 : 0);

  HotKey* h = hotkeys;
  for (; h != NULL; h = h->next) {
    if (h->keysym == keysym && ev->xkey.state == h->modifiers) {
      shell(h->command);
      break;
    }
  }
}

bool forceRestart;
#define NullEvent -1

static void getEvent(XEvent* ev) {
  // Is there a message waiting?
  if (QLength(dpy) > 0) {
    XNextEvent(dpy, ev);
    return;
  }

  // Beg...
  XFlush(dpy);

  // Wait one second to see if a message arrives.
  int fd = ConnectionNumber(dpy);
  fd_set readfds;
  FD_ZERO(&readfds);
  FD_SET(fd, &readfds);
  struct timeval tv = {.tv_sec = 1};
  if (select(fd + 1, &readfds, 0, 0, &tv) == 1) {
    XNextEvent(dpy, ev);
    return;
  }

  // No message, so we have a null event.
  ev->type = NullEvent;
}

// If we execvp ourselves in the signal handler itself, it seems to prevent
// future signals of the same type from being delivered. So we need this
// convoluted use of a global variable to detect whether we need to restart,
// along with a more complicated 'getEvent' function to ensure we frequently
// wake up and check for forceRestart being set in the main loop.
void RestartSelf(int signum) {
  forceRestart = true;
}

int main(int argc, char* argv[]) {
  argv0 = argv[0];
  hot_key_file = argv[1];

  // Set SIGHUP to make us exec ourselves. This provides a nice easy way for the
  // user to make us reload our config file.
  struct sigaction sa = {};
  sa.sa_handler = RestartSelf;
  sigaddset(&(sa.sa_mask), SIGHUP);
  if (sigaction(SIGHUP, &sa, NULL)) {
    fprintf(stderr, "SIGHUP sigaction failed: %d\n", errno);
  }

  /* Open a connection to the X server. */
  dpy = XOpenDisplay("");
  if (dpy == 0) {
    panic("can't open display.");
  }

  /* Set up signal handlers. */
  signal(SIGCHLD, sigchld_handler);

  screen_count = ScreenCount(dpy);

  if (argc != 2) {
    panic("syntax: speckeysd <keys file>");
  }

  read_hot_key_file(hot_key_file);

  /* Make sure all our communication to the server got through. */
  XSync(dpy, False);

  /* The main event loop. */
  while (!forceRestart) {
    XEvent ev;
    getEvent(&ev);
    switch (ev.type) {
      case KeyPress:
        keypress(&ev);
        break;
      case MappingNotify:
        XRefreshKeyboardMapping((XMappingEvent*)&ev);
        break;
      default:
        /* Do I look like I care? */
        break;
    }
  }
  execvp(argv[0], argv);
}
