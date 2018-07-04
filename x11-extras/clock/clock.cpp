#define DEFAULT_FONT "times-14"

#include <ctype.h>
#include <errno.h>
#include <error.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include <unistd.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/wait.h>

#include <X11/Xlib.h>
#include <X11/Xresource.h>
#include <X11/Xutil.h>

#include <string>

// The display.
static Display* g_display;

// The width of the screen we're managing.
static int display_width;
// The height of the screen we're managing.
static int display_height;
// The width of our window.
static int window_width;
// The height of our window.
static int window_height;

static Window window; /* Main window. */
static Window root;   /* Root window. */
static GC gc;         /* The default GC. */

// Font.
static XFontStruct* font;

// Live on the left rather than the right?
static bool sinister = false;

// User's view command.
static char* view_command = nullptr;

// User's font.
static std::string font_name{DEFAULT_FONT};

// User's button commands (element 0 unused).
static char* command[6] = {};

static char* display_string = nullptr;

static void update_display_string();
static char* pipe_command(char*);

static char* sdup(const char* p) {
  char* s = strdup(p);
  if (s == 0) {
    error(1, errno, "strdup failed");
  }
  return s;
}

static void update_display_string() {
  if (display_string != nullptr) {
    free(display_string);
  }

  if (view_command != nullptr) {
    display_string = pipe_command(view_command);
  } else {
    // Fall back to computing the date & time internally.
    time_t t = time(nullptr);
    struct tm tm = *localtime(&t);
    char time_string[17];  // "1234-67-90 23:56"
    strftime(time_string, sizeof(time_string), "%Y-%m-%d %H:%M", &tm);

    display_string = sdup(time_string);
  }

  // Recompute the window width.
  window_width =
      4 * XTextWidth(font, display_string, strlen(display_string)) / 3;
}

static void DoExpose(XEvent* ev) {
  // Only handle the last in a group of Expose events.
  if ((ev && ev->xexpose.count != 0) || (display_string == 0))
    return;

  // Do the redraw.
  int width = XTextWidth(font, display_string, strlen(display_string));
  XDrawString(g_display, ev->xexpose.window, gc, (window_width - width) / 2,
              window_height / 4 + font->ascent, display_string,
              strlen(display_string));
}

static void DoMappingNotify(XEvent* ev) {
  XRefreshKeyboardMapping((XMappingEvent*)ev);
}

static void DoEnter(XEvent* ev) {
  update_display_string();
  XMoveResizeWindow(g_display, ev->xcrossing.window,
                    sinister ? 0 : display_width - window_width, 0,
                    window_width, window_height);
}

static void DoLeave(XEvent* ev) {
  XMoveResizeWindow(g_display, ev->xcrossing.window,
                    sinister ? 0 : display_width - 2, 0, 2, 2);
}

static void DoCommand(XEvent* ev) {
  int button = ev->xbutton.button;
  if (command[button] == nullptr)
    return;

  static const char* sh;
  if (sh == nullptr) {
    sh = getenv("SHELL");
    if (sh == nullptr)
      sh = "/bin/sh";
  }

  switch (fork()) {
    case 0:  // Child.
      close(ConnectionNumber(g_display));
      switch (fork()) {
        case 0:
          execl(sh, sh, "-c", command[button], nullptr);
          error(1, errno, "exec \"%s -c %s\" failed", sh, command[button]);
        case -1:
          error(1, errno, "fork failed");
        default:
          _exit(EXIT_SUCCESS);
      }
    case -1:  // Error.
      error(0, errno, "fork failed");
      break;
    default:
      wait(0);
  }
}

static char* pipe_command(char* command) {
  static const char* sh;
  int fds[2];
  char* string;

  if (sh == nullptr) {
    sh = getenv("SHELL");
    if (sh == nullptr)
      sh = "/bin/sh";
  }

  if (pipe(fds) == -1)
    return strdup("Execution failed");

  switch (fork()) {
    case 0: /* Child. */
      close(ConnectionNumber(g_display));
      switch (fork()) {
        case 0:
          close(0);
          close(fds[0]);
          dup2(fds[1], 1);
          dup2(1, 2);

          execl(sh, sh, "-c", command, nullptr);
          error(1, errno, "exec \"%s -c %s\" failed", sh, command);
          exit(EXIT_FAILURE);
        case -1:
          error(1, errno, "fork failed");
        default:
          _exit(EXIT_SUCCESS);
      }
    case -1:  // Error.
      error(1, errno, "fork failed");
      break;
    default: {
      /* Read from the pipe. */
      char buf[BUFSIZ];
      int n;

      close(fds[1]);

      while ((n = read(fds[0], buf, BUFSIZ)) > 0) {
        int valid = 0;
        char* p = buf;

        /* How many of the characters do we want? */
        while (*p++ >= ' ') {
          valid++;
        }

        string = (char*)malloc(valid + 1);
        if (string != 0) {
          strncpy(string, buf, valid);
        } else {
          string = strdup("Out of memory");
        }
      }

      wait(0);
    }
  }

  return string;
}

static void DoVisibilityNotify(XEvent* ev) {
  if (ev->xvisibility.state == VisibilityUnobscured) {
    return;
  }

  // Is it lock?
  Window root_return, parent_return;
  Window* wins;
  unsigned int nwins;
  XQueryTree(g_display, root, &root_return, &parent_return, &wins, &nwins);
  char* name;
  XFetchName(g_display, wins[nwins - 1], &name);
  if (name == 0 || strcmp(name, "lock"))
    XRaiseWindow(g_display, window);

  XFree(wins);
  XFree(name);
}

static int ErrorHandler(Display* d, XErrorEvent* e) {
  char msg[80];
  XGetErrorText(d, e->error_code, msg, sizeof(msg));

  char number[80];
  snprintf(number, sizeof(number), "%d", e->request_code);

  char req[80];
  XGetErrorDatabaseText(d, "XRequest", number, number, req, sizeof(req));

  error(0, 0, "protocol request %s on resource %#lx failed: %s", req,
        e->resourceid, msg);
  return 0;
}

static void GetResources() {
  char* resource_manager = XResourceManagerString(g_display);
  if (resource_manager == nullptr) {
    return;
  }

  XrmInitialize();
  XrmDatabase db = XrmGetStringDatabase(resource_manager);
  if (db == nullptr) {
    return;
  }

  // Font.
  XrmValue value;
  char* type;
  if (XrmGetResource(db, "clock.font", "Font", &type, &value) == True) {
    if (strcmp(type, "String") == 0) {
      font_name = value.addr;
    }
  }

  // Button commands.
  for (int i = 1; i < 6; i++) {
    char resource[15];
    sprintf(resource, "clock.button%i", i);
    if (XrmGetResource(db, resource, "String", &type, &value) == True) {
      if (strcmp(type, "String") == 0) {
        command[i] = sdup(value.addr);
      }
    }
  }

  // View command.
  if (XrmGetResource(db, "clock.viewCommand", "String", &type, &value) ==
      True) {
    if (strcmp(type, "String") == 0) {
      view_command = sdup(value.addr);
    }
  }

  // Leftist tendency?
  if (XrmGetResource(db, "clock.leftHanded", "Existential", &type, &value) ==
      True) {
    sinister = true;
  }
}

int main(int argc, char* argv[]) {
  // Open a connection to the X server.
  g_display = XOpenDisplay("");
  if (g_display == nullptr) {
    error(1, 0, "can't open display");
  }

  GetResources();

  // Find the screen's dimensions.
  display_width = DisplayWidth(g_display, DefaultScreen(g_display));
  display_height = DisplayHeight(g_display, DefaultScreen(g_display));

  // Set up an error handler.
  XSetErrorHandler(ErrorHandler);

  // Get the pixel values of the only two colours we use.
  unsigned long black = BlackPixel(g_display, DefaultScreen(g_display));
  unsigned long white = WhitePixel(g_display, DefaultScreen(g_display));

  // Get font.
  font = XLoadQueryFont(g_display, font_name.c_str());
  if (font == nullptr) {
    error(0, 0, "couldn't find font %s; falling back", font_name.c_str());
    font = XLoadQueryFont(g_display, "fixed");
  }
  if (font == nullptr) {
    error(1, 0, "can't find a font");
  }

  window_width = 100; /* Arbitrary: ephemeral. */
  window_height = 2 * (font->ascent + font->descent);

  // Create the window.
  root = DefaultRootWindow(g_display);
  XSetWindowAttributes attr;
  attr.override_redirect = True;
  attr.background_pixel = black;
  attr.border_pixel = black;
  attr.event_mask = ExposureMask | VisibilityChangeMask | ButtonPressMask |
                    ButtonReleaseMask | StructureNotifyMask | EnterWindowMask |
                    LeaveWindowMask;
  window = XCreateWindow(
      g_display, root, sinister ? 0 : display_width - 2, 0, 2, 2, 0,
      CopyFromParent, InputOutput, CopyFromParent,
      CWOverrideRedirect | CWBackPixel | CWBorderPixel | CWEventMask, &attr);

  // Create GC.
  XGCValues gv;
  gv.foreground = white;
  gv.background = black;
  gv.font = font->fid;
  gc = XCreateGC(g_display, window, GCForeground | GCBackground | GCFont, &gv);

  // Bring up the window.
  XMapRaised(g_display, window);

  // Make sure all our communication to the server got through.
  XSync(g_display, False);

  // The main event loop.
  while (true) {
    XEvent ev;
    XNextEvent(g_display, &ev);
    switch (ev.type) {
      case ButtonRelease:
        DoCommand(&ev);
        break;
      case Expose:
        DoExpose(&ev);
        break;
      case EnterNotify:
        DoEnter(&ev);
        break;
      case LeaveNotify:
        DoLeave(&ev);
        break;
      case MappingNotify:
        DoMappingNotify(&ev);
        break;
      case VisibilityNotify:
        DoVisibilityNotify(&ev);
        break;
    }
  }
}
