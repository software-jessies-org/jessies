#include <ctype.h>
#include <error.h>
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

#define NullEvent -1

// User's command to run instead of just showing the clock. This may be
// enhanced to show information other than a mere clock, for example it could
// be a script (on a laptop) which displays the battery charge level alongside
// a clock. The command will be run once per second, and the resulting text
// shown in the menu.
static char* clock_command;
// User's font.
static std::string g_font_name{"roboto-16"};

struct MenuItem {
  MenuItem* next;

  char* name;
  char* command;
};

// The connection to the X server.
static Display* dpy;
static int display_width;

// Main window.
static Window window;
static int window_height;
// Root window.
static Window root;
// The default GC.
static GC gc;

// Black pixel.
static unsigned long black;
// White pixel.
static unsigned long white;

// Font.
static XftFont* g_font;
static XftDraw* g_font_draw;
static XftColor g_font_color;

static MenuItem* menu = 0;
static MenuItem* selected = 0;

static bool forceRestart;

static void getEvent(XEvent*);

// By default, we'll only use a handful of bytes in the below time_string,
// but if we're asked to run an external command to fill it in, we'll allow
// up to 1024 bytes from the sub-command.
static char time_string[1024];  // "1234-67-90 23:56".

static void Execute(MenuItem* item) {
  if (item->command == 0) {
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
      close(ConnectionNumber(dpy));
      switch (fork()) {
        case 0:
          execl(sh, sh, "-c", item->command, (char*)NULL);
          error(1, errno, "exec \"%s -c %s\" failed", sh, item->command);
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

static void RunClockCommand() {
  FILE* fp = popen(clock_command, "r");
  if (fp == NULL) {
    snprintf(time_string, sizeof time_string, "Failed to run command '%s'",
             clock_command);
    return;
  }
  // Read up to 1024 (well, 1023+terminator) bytes from the sub-process;
  // we will refuse to display more than that, as screens are only so large.
  size_t numRead =
      fread(time_string, sizeof(char), sizeof(time_string) - 1, fp);
  time_string[numRead] = '\0';
  // Turn all newlines into spaces, as newlines don't make sense in a single
  // horizontal line. On the other hand, they're a natural thing for a script
  // to spit out, so easier to cope with them here than to have to do funky
  // stuff in script land.
  for (int i = 0; time_string[i]; i++) {
    if (time_string[i] == '\n' || time_string[i] == '\r') {
      // If this newline is the last character in the string, just kill it
      // with a terminator, rather than adding a space at the end of the
      // string.
      time_string[i] = time_string[i + 1] ? ' ' : '\0';
    }
  }
  pclose(fp);
}

static void UpdateTime() {
  if (clock_command) {
    RunClockCommand();
    return;
  }
  time_t t = time(nullptr);
  struct tm* tm = localtime(&t);
  strftime(time_string, sizeof(time_string), "%Y-%m-%d %H:%M", tm);
}

static int TextWidth(const char* s) {
  XGlyphInfo extents;
  XftTextExtentsUtf8(dpy, g_font, reinterpret_cast<const FcChar8*>(s),
                     strlen(s), &extents);
  return extents.xOff;
}

static void DoExpose(XEvent* ev) {
  // Only handle the last in a group of Expose events.
  if (ev && ev->xexpose.count != 0) {
    return;
  }

  // Clear the window.
  XClearWindow(dpy, window);

  // Shave off the corners.
  XFillRectangle(dpy, window, gc, 0, 0, 10, 10);
  XFillRectangle(dpy, window, gc, display_width - 10, 0, 10, 10);
  XSetForeground(dpy, gc, white);
  XFillArc(dpy, window, gc, 0, 0, 18, 18, 0, 360 * 64);
  XFillArc(dpy, window, gc, display_width - 18, 0, 18, 18, 0, 360 * 64);
  XSetForeground(dpy, gc, black);

  // Draw the menu.
  int x = 20;
  for (MenuItem* item = menu; item != 0; item = item->next) {
    int width = TextWidth(item->name);
    XftDrawStringUtf8(
        g_font_draw, &g_font_color, g_font, x, 1.1 * g_font->ascent,
        reinterpret_cast<const FcChar8*>(item->name), strlen(item->name));
    if (item == selected) {
      XSetFunction(dpy, gc, GXinvert);
      XFillRectangle(dpy, window, gc, x - 5, 0, width + 10, window_height);
      XSetFunction(dpy, gc, GXcopy);
    }
    x += 20 + width;
  }

  // Draw the clock.
  UpdateTime();
  int width = TextWidth(time_string);
  XftDrawStringUtf8(g_font_draw, &g_font_color, g_font,
                    display_width - width - 20, 1.2 * g_font->ascent,
                    reinterpret_cast<const FcChar8*>(time_string),
                    strlen(time_string));
}

static void DoNullEvent(XEvent* ev) {
  // Ensure that the clock is redrawn.
  DoExpose(nullptr);
}

static MenuItem* WhichItem(int mouseX) {
  int x = 0;
  for (MenuItem* item = menu; item != 0; item = item->next) {
    int itemWidth = 20 + TextWidth(item->name);
    if (mouseX >= x && mouseX <= (x + itemWidth)) {
      return item;
    }
    x += itemWidth;
  }

  return 0;
}

static void DoMouseMoved(XEvent* ev) {
  while (XCheckMaskEvent(dpy, ButtonMotionMask, ev)) {
  }

  if (!XQueryPointer(dpy, ev->xmotion.window, &(ev->xmotion.root),
                     &(ev->xmotion.subwindow), &(ev->xmotion.x_root),
                     &(ev->xmotion.y_root), &(ev->xmotion.x), &(ev->xmotion.y),
                     &(ev->xmotion.state))) {
    return;
  }

  selected = WhichItem(ev->xmotion.x);
  DoExpose(nullptr);
}

static void DoButtonPress(XEvent* ev) {
  selected = WhichItem(ev->xbutton.x);
  DoExpose(nullptr);
}

static void DoButtonRelease(XEvent* ev) {
  if (selected != 0) {
    Execute(selected);
    selected = 0;
  }
  DoExpose(nullptr);
}

static void DoMappingNotify(XEvent* ev) {
  XRefreshKeyboardMapping((XMappingEvent*)ev);
}

static void DoLeave(XEvent* ev) {
  selected = 0;
  DoExpose(nullptr);
}

int ErrorHandler(Display* d, XErrorEvent* e) {
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

extern void GetResources() {
  char* resource_manager = XResourceManagerString(dpy);
  if (resource_manager == 0) {
    return;
  }

  XrmInitialize();
  XrmDatabase db = XrmGetStringDatabase(resource_manager);
  if (db == nullptr) {
    return;
  }

  // Font.
  char* type;
  XrmValue value;
  if (XrmGetResource(db, "menu.font", "Font", &type, &value) == True) {
    if (strcmp(type, "String") == 0) {
      g_font_name = value.addr;
    }
  }

  // Command to run to show the time/status on the right side of the menu.
  if (XrmGetResource(db, "menu.clockCommand", "String", &type, &value) ==
      True) {
    if (strcmp(type, "String") == 0) {
      clock_command = strdup((char*)value.addr);
    }
  }
}

static void RestartSelf(int signum) {
  forceRestart = true;
}

static void AddMenuItem(const char* name, const char* command) {
  MenuItem* newItem = (MenuItem*)malloc(sizeof(MenuItem));
  if (newItem == 0) {
    return;
  }

  newItem->next = 0;
  newItem->name = strdup(name);
  newItem->command = strdup(command);

  if (menu == 0) {
    menu = newItem;
  } else {
    MenuItem* last = menu;
    while (last->next != 0) {
      last = last->next;
    }
    last->next = newItem;
  }
}

static void ReadMenu() {
  FILE* fp = fopen(".menu", "r");
  if (fp == nullptr) {
    AddMenuItem("localhost", "exec xterm");
    return;
  }

  char line[BUFSIZ];
  while (fgets(line, BUFSIZ, fp) != 0) {
    char* tab = strchr(line, '\t');
    if (*line == '#' || tab == 0)
      continue;
    *tab = 0;
    AddMenuItem(line, tab + 1);
  }

  fclose(fp);
}

int main(int argc, char* argv[]) {
  struct sigaction sa = {};
  sa.sa_handler = RestartSelf;
  sigaddset(&(sa.sa_mask), SIGHUP);
  if (sigaction(SIGHUP, &sa, NULL)) {
    error(1, errno, "SIGHUP sigaction failed");
  }

  // Open a connection to the X server.
  dpy = XOpenDisplay("");
  if (dpy == 0) {
    error(1, 0, "can't open display");
  }

  GetResources();

  // Find the screen's dimensions.
  int screen = DefaultScreen(dpy);
  display_width = DisplayWidth(dpy, screen);

  // Set up an error handler.
  XSetErrorHandler(ErrorHandler);

  // Get the pixel values of the only two colours we use.
  black = BlackPixel(dpy, screen);
  white = WhitePixel(dpy, screen);

  // Get font.
  g_font = XftFontOpenName(dpy, screen, g_font_name.c_str());
  if (g_font == nullptr) {
    error(0, 0, "couldn't find font %s; falling back", g_font_name.c_str());
    g_font = XftFontOpenName(dpy, 0, "fixed");
    if (g_font == nullptr) {
      error(1, 0, "can't find a font");
    }
  }

  // Create the window.
  window_height = 1.2 * (g_font->ascent + g_font->descent);
  root = DefaultRootWindow(dpy);
  XSetWindowAttributes attr;
  attr.override_redirect = True;
  attr.background_pixel = white;
  attr.border_pixel = black;
  attr.event_mask = ExposureMask | VisibilityChangeMask | ButtonMotionMask |
                    PointerMotionHintMask | ButtonPressMask |
                    ButtonReleaseMask | StructureNotifyMask | EnterWindowMask |
                    LeaveWindowMask;
  window = XCreateWindow(
      dpy, root, 0, 0, display_width, window_height, 0, CopyFromParent,
      InputOutput, CopyFromParent,
      CWOverrideRedirect | CWBackPixel | CWBorderPixel | CWEventMask, &attr);

  // Create the objects needed to render text in the window.
  g_font_draw = XftDrawCreate(dpy, window, DefaultVisual(dpy, screen),
                              DefaultColormap(dpy, screen));
  XRenderColor xrc = {.red = 0, .green = 0, .blue = 0, .alpha = 0xffff};
  XftColorAllocValue(dpy, DefaultVisual(dpy, screen),
                     DefaultColormap(dpy, screen), &xrc, &g_font_color);

  // Create GC.
  XGCValues gv;
  gv.foreground = black;
  gv.background = white;
  gc = XCreateGC(dpy, window, GCForeground | GCBackground, &gv);

  // Create the menu items.
  ReadMenu();

  // Bring up the window.
  XMapRaised(dpy, window);

  // Make sure all our communication to the server got through.
  XSync(dpy, False);

  // The main event loop.
  while (!forceRestart) {
    XEvent ev;
    getEvent(&ev);
    switch (ev.type) {
      case NullEvent:
        DoNullEvent(&ev);
        break;
      case ButtonPress:
        DoButtonPress(&ev);
        break;
      case ButtonRelease:
        DoButtonRelease(&ev);
        break;
      case MotionNotify:
        DoMouseMoved(&ev);
        break;
      case Expose:
        DoExpose(&ev);
        break;
      case LeaveNotify:
        DoLeave(&ev);
        break;
      case MappingNotify:
        DoMappingNotify(&ev);
        break;
    }
  }

  // Someone hit us with a SIGHUP: better exec ourselves to force a config
  // reload and cope with changing screen sizes.
  execvp(argv[0], argv);
}

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
