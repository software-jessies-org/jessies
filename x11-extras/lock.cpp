#include <ctype.h>
#include <errno.h>
#include <error.h>
#include <pwd.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#include <string>

#include <X11/X.h>
#include <X11/Xatom.h>
#include <X11/Xlib.h>
#include <X11/Xproto.h>
#include <X11/Xresource.h>
#include <X11/Xutil.h>
#include <X11/keysym.h>

#ifdef __linux__
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/vt.h>
#endif

struct ScreenInfo {
  Window window;
  GC gc;

  // Screen dimensions.
  int width, height;

  // Pixel values.
  unsigned long black, white;
};

// User's font.
static std::string g_font_name{
    "-*-lucida-bold-r-normal-sans-*-250-*-*-*-*-*-*"};
// User's string for "locked" message.
static std::string g_lock_string{
    "This X display is locked. Please supply the password."};

// The connection to the X server.
static Display* dpy;
// The number of screens.
static int screen_count;

static ScreenInfo* screens;
static XFontStruct* font;

static int ignore_badwindow;

static char password[14];
static char stars[14] = "*************";
static int chars = 0;

// TODO: switch to PAM.
// Modern systems don't leave the password field readable.
// They also don't allow non-root access to /etc/shadow.
// You'd need to rewrite this to use PAM these days.
static bool CheckPassword(const char* p) {
  if (p == 0)
    return false;

  char* logname = getenv("LOGNAME");
  if (logname == nullptr) {
    logname = getenv("USER");
  }
  if (logname == nullptr) {
    logname = getlogin();
  }

  passwd* spw = getpwnam(logname);
  if (spw == nullptr || spw->pw_passwd == nullptr)
    return false;

  char salt[3];
  strncpy(salt, spw->pw_passwd, 2);
  salt[2] = 0;

  char* encrypted = crypt(p, salt);
  if (encrypted == 0)
    return 0;

  return !strcmp(spw->pw_passwd, encrypted);
}

static ScreenInfo* GetScreenForWindow(Window w) {
  for (int screen = 0; screen < screen_count; screen++) {
    if (screens[screen].window == w) {
      return &screens[screen];
    }
  }
  return 0;
}

static void ExposeScreen(ScreenInfo* screen) {
  if (screen != &screens[0])
    return;

  // Do the redraw.
  int width = XTextWidth(font, g_lock_string.c_str(), g_lock_string.size());
  XDrawString(dpy, screen->window, screen->gc, (screen->width - width) / 2,
              screen->height / 4, g_lock_string.c_str(), g_lock_string.size());

  width = XTextWidth(font, stars, chars);
  XSetForeground(dpy, screen->gc, screen->black);
  XFillRectangle(dpy, screen->window, screen->gc, 0, screen->height / 2 - 100,
                 screen->width, 100);
  XSetForeground(dpy, screen->gc, screen->white);
  XDrawString(dpy, screen->window, screen->gc, (screen->width - width) / 2,
              screen->height / 2, stars, chars);
}

static void DoExpose(XEvent* ev) {
  // Only handle the last in a group of Expose events.
  if (ev->xexpose.count != 0) {
    return;
  }

  ExposeScreen(GetScreenForWindow(ev->xexpose.window));
}

static void DoKeyPress(XEvent* ev) {
  char buffer[1];
  KeySym keysym;
  int c = XLookupString((XKeyEvent*)ev, buffer, sizeof(buffer), &keysym, 0);
  if (keysym == XK_Return) {
    password[chars] = 0;
    if (CheckPassword(password)) {
      XUngrabKeyboard(dpy, CurrentTime);
#ifdef __linux__
      {
        int fd = open("/dev/console", O_RDWR);
        if (fd != -1) {
          ioctl(fd, VT_UNLOCKSWITCH, 0);
        }
      }
#endif
      exit(EXIT_SUCCESS);
    } else {
      XBell(dpy, 100);
      chars = 0;
    }
  } else if (keysym == XK_BackSpace) {
    if (chars > 0) {
      chars--;
    }
  } else if (keysym == XK_Escape) {
    chars = 0;
  } else if (c > 0 && isprint(*buffer) && chars + 1 <= 13) {
    password[chars++] = *buffer;
  }
  ExposeScreen(&screens[0]);
}

static void DoMappingNotify(XEvent* ev) {
  XRefreshKeyboardMapping((XMappingEvent*)ev);
}

static void RaiseLockWindows(void) {
  for (int screen = 0; screen < screen_count; screen++) {
    XRaiseWindow(dpy, screens[screen].window);
  }
}

static int ErrorHandler(Display* d, XErrorEvent* e) {
  if (ignore_badwindow &&
      (e->error_code == BadWindow || e->error_code == BadColor)) {
    return 0;
  }

  char msg[80];
  XGetErrorText(d, e->error_code, msg, sizeof(msg));
  char number[80];
  snprintf(number, sizeof(number), "%d", e->request_code);
  char req[80];
  XGetErrorDatabaseText(d, "XRequest", number, number, req, sizeof(req));

  error(1, 0, "protocol request %s on resource %#lx failed: %s\n", req,
        e->resourceid, msg);

  return 0;
}

static void GetResources() {
  char* resource_manager = XResourceManagerString(dpy);
  if (resource_manager == nullptr)
    return;

  XrmInitialize();
  XrmDatabase db = XrmGetStringDatabase(resource_manager);
  if (db == nullptr)
    return;

  // Font.
  XrmValue value;
  char* type;
  if (XrmGetResource(db, "lock.font", "Font", &type, &value) == True) {
    if (strcmp(type, "String") == 0) {
      g_font_name = value.addr;
    }
  }

  // Lock string.
  if (XrmGetResource(db, "lock.message", "String", &type, &value) == True) {
    if (strcmp(type, "String") == 0) {
      g_lock_string = value.addr;
    }
  }
}

static void InitScreen(int screen) {
  XGCValues gv;
  XSetWindowAttributes attr;
  Cursor no_cursor;
  XColor colour;

  // Find the screen's dimensions.
  screens[screen].width = DisplayWidth(dpy, screen);
  screens[screen].height = DisplayHeight(dpy, screen);

  // Get the pixel values of the only two colors we use.
  screens[screen].black = BlackPixel(dpy, screen);
  screens[screen].white = WhitePixel(dpy, screen);

  // Create the locking window.
  attr.override_redirect = True;
  attr.background_pixel = screens[screen].black;
  attr.border_pixel = screens[screen].black;
  attr.event_mask = ExposureMask | VisibilityChangeMask | StructureNotifyMask;
  attr.do_not_propagate_mask = ButtonPressMask | ButtonReleaseMask;
  screens[screen].window = XCreateWindow(
      dpy, RootWindow(dpy, screen), 0, 0, screens[screen].width,
      screens[screen].height, 0, CopyFromParent, InputOutput, CopyFromParent,
      CWOverrideRedirect | CWBackPixel | CWBorderPixel | CWEventMask |
          CWDontPropagate,
      &attr);

  XStoreName(dpy, screens[screen].window, "lock");

  // Create GC.
  gv.foreground = screens[screen].white;
  gv.background = screens[screen].black;
  gv.font = font->fid;
  screens[screen].gc = XCreateGC(dpy, screens[screen].window,
                                 GCForeground | GCBackground | GCFont, &gv);

  // Hide the cursor.
  no_cursor =
      XCreateGlyphCursor(dpy, font->fid, font->fid, ' ', ' ', &colour, &colour);
  XDefineCursor(dpy, screens[screen].window, no_cursor);

  // Bring up the lock window.
  XMapRaised(dpy, screens[screen].window);
}

static void InitScreens() {
  // Find out how many screens we've got, and allocate space for their info.
  screen_count = ScreenCount(dpy);
  screens = (ScreenInfo*)malloc(sizeof(ScreenInfo) * screen_count);

  // Go through the screens one-by-one, initializing them.
  for (int screen = 0; screen < screen_count; screen++) {
    InitScreen(screen);
  }
}

int main(int argc, char* argv[]) {
  // Open a connection to the X server.
  dpy = XOpenDisplay("");
  if (dpy == 0)
    error(1, 0, "can't open display");

  GetResources();

  // Set up an error handler.
  XSetErrorHandler(ErrorHandler);

  // Set up signal handlers.
  signal(SIGTERM, SIG_IGN);
  signal(SIGINT, SIG_IGN);
  signal(SIGHUP, SIG_IGN);

  // Get font.
  font = XLoadQueryFont(dpy, g_font_name.c_str());
  if (font == 0)
    font = XLoadQueryFont(dpy, "fixed");
  if (font == 0)
    error(1, 0, "can't find a font");

  InitScreens();

  // Grab the keyboard.
  XGrabKeyboard(dpy, screens[0].window, False, GrabModeAsync, GrabModeAsync,
                CurrentTime);
#ifdef __linux__
  {
    int fd = open("/dev/console", O_RDWR);
    if (fd != -1) {
      ioctl(fd, VT_LOCKSWITCH, 0);
      // close(fd);
    }
    fprintf(stderr, "/dev/console = %i\n", fd);
  }
#endif

  // Make sure all our communication to the server got through.
  XSync(dpy, False);

  // The main event loop.
  for (;;) {
    XEvent ev;
    XNextEvent(dpy, &ev);
    switch (ev.type) {
      case Expose:
        DoExpose(&ev);
      case KeyPress:
        DoKeyPress(&ev);
      case MappingNotify:
        DoMappingNotify(&ev);
    }
    RaiseLockWindows();
  }
}
