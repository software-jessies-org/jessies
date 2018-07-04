#include <ctype.h>
#include <errno.h>
#include <error.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

#include <string>

#include <X11/Xft/Xft.h>
#include <X11/Xlib.h>
#include <X11/Xresource.h>
#include <X11/Xutil.h>

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

// Font.
static XftFont* g_font;
static XftDraw* g_font_draw;
static XftColor g_font_color;

// Live on the left rather than the right?
static bool sinister = false;

// User's view command.
static std::string g_view_command;
// User's font.
static std::string g_font_name{"roboto-16"};
// User's button commands (element 0 unused).
static char* command[6] = {};

static std::string display_string;

static char* xstrdup(const char* p) {
  char* s = strdup(p);
  if (s == 0) {
    error(1, errno, "strdup failed");
  }
  return s;
}

static std::string PipeCommand(const std::string& command) {
  FILE* fp = popen(command.c_str(), "re");
  if (fp == nullptr) {
    return "";
  }
  // Read up to 1024 (well, 1023+terminator) bytes from the sub-process;
  // we will refuse to display more than that, as screens are only so large.
  char buf[BUFSIZ];
  size_t byte_count = fread(buf, 1, sizeof(buf) - 1, fp);
  buf[byte_count] = '\0';
  char* p = buf;
  // Turn all newlines into spaces, as newlines don't make sense in a single
  // horizontal line. On the other hand, they're a natural thing for a script
  // to spit out, so easier to cope with them here than to have to do funky
  // stuff in script land.
  for (; *p; ++p) {
    if (*p < ' ') {
      *p = ' ';
    }
  }
  // Trim any trailing whitespace.
  while (p > buf && isspace(*(p - 1))) {
    *--p = '\0';
  }
  pclose(fp);
  return buf;
}

static void UpdateDisplayString() {
  if (!g_view_command.empty()) {
    display_string = PipeCommand(g_view_command);
  } else {
    // Fall back to computing the date & time internally.
    time_t t = time(nullptr);
    struct tm tm = *localtime(&t);
    char time_string[17];  // "1234-67-90 23:56"
    strftime(time_string, sizeof(time_string), "%Y-%m-%d %H:%M", &tm);
    display_string = time_string;
  }

  // Recompute the window width.
  XGlyphInfo extents;
  XftTextExtentsUtf8(g_display, g_font,
                     reinterpret_cast<const FcChar8*>(display_string.c_str()),
                     display_string.size(), &extents);
  window_width = (4 * extents.xOff) / 3;
}

static void DoExpose(XEvent* ev) {
  // Only handle the last in a group of Expose events.
  if ((ev && ev->xexpose.count != 0) || display_string.empty()) {
    return;
  }

  // Do the redraw.
  int text_width = (window_width * 3) / 4;
  XftDrawStringUtf8(
      g_font_draw, &g_font_color, g_font, (window_width - text_width) / 2,
      window_height / 4 + g_font->ascent,
      reinterpret_cast<const FcChar8*>(display_string.c_str()),
      display_string.size());
}

static void DoMappingNotify(XEvent* ev) {
  XRefreshKeyboardMapping((XMappingEvent*)ev);
}

static void DoEnter(XEvent* ev) {
  UpdateDisplayString();
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
  if (command[button] == nullptr) {
    return;
  }

  static const char* sh;
  if (sh == nullptr) {
    sh = getenv("SHELL");
    if (sh == nullptr) {
      sh = "/bin/sh";
    }
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
  if (name == 0 || strcmp(name, "lock")) {
    XRaiseWindow(g_display, window);
  }

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
      g_font_name = value.addr;
    }
  }

  // Button commands.
  for (int i = 1; i < 6; i++) {
    char resource[15];
    sprintf(resource, "clock.button%i", i);
    if (XrmGetResource(db, resource, "String", &type, &value) == True) {
      if (strcmp(type, "String") == 0) {
        command[i] = xstrdup(value.addr);
      }
    }
  }

  // View command.
  if (XrmGetResource(db, "clock.viewCommand", "String", &type, &value) ==
      True) {
    if (strcmp(type, "String") == 0) {
      g_view_command = value.addr;
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
  int screen = DefaultScreen(g_display);
  display_width = DisplayWidth(g_display, screen);
  display_height = DisplayHeight(g_display, screen);

  // Set up an error handler.
  XSetErrorHandler(ErrorHandler);

  // Get the pixel values of the only two colours we use.
  unsigned long black = BlackPixel(g_display, screen);

  // Get font.
  g_font = XftFontOpenName(g_display, screen, g_font_name.c_str());
  if (g_font == nullptr) {
    error(0, 0, "couldn't find font %s; falling back", g_font_name.c_str());
    g_font = XftFontOpenName(g_display, 0, "fixed");
    if (g_font == nullptr) {
      error(1, 0, "can't find a font");
    }
  }

  window_width = 100;  // Arbitrary: ephemeral.
  window_height = 2 * (g_font->ascent + g_font->descent);

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

  // Create the objects needed to render text in the window.
  g_font_draw =
      XftDrawCreate(g_display, window, DefaultVisual(g_display, screen),
                    DefaultColormap(g_display, screen));
  XRenderColor xrc = {
      .red = 0xffff, .green = 0xffff, .blue = 0xffff, .alpha = 0xffff};
  XftColorAllocValue(g_display, DefaultVisual(g_display, screen),
                     DefaultColormap(g_display, screen), &xrc, &g_font_color);

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
