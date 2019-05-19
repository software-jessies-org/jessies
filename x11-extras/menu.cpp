#include <ctype.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

#include <map>
#include <string>
#include <vector>

#include <X11/Xatom.h>
#include <X11/Xft/Xft.h>
#include <X11/Xlib.h>
#include <X11/Xresource.h>

#include <X11/extensions/Xrandr.h>

#include "log.h"

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
          LOGF() << "exec \"" << sh << " -c " << item->command
                 << "\" failed: " << Log::Errno(errno);
        case -1:
          LOGF() << "fork failed: " << Log::Errno(errno);
        default:
          _exit(EXIT_SUCCESS);
      }
    case -1:  // Error.
      LOGE() << "fork failed: " << Log::Errno(errno);
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

  LOGE() << "protocol request " << req << " on resource " << std::hex
         << e->resourceid << " failed: " << msg;
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

static void setWindowProperty(Window window,
                              const char* name,
                              unsigned long* val,
                              int len) {
  Atom prop = XInternAtom(dpy, name, true);
  if (prop == None) {
    LOGE() << "no such property '" << name << "'";
    return;
  }
  XChangeProperty(dpy, window, prop, XA_CARDINAL, 32, PropModeReplace,
                  (unsigned char*)val, len);
}

static void setWindowProps(Window window, int x, int y, int width, int height) {
  // _NET_WM_WINDOW_TYPE describes that this is a kind of dock or panel window,
  // that should probably be kept on top, but that the window manager certainly
  // shouldn't decorate with frame, title bar etc.
  Atom typeAtom = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE", 0);
  Atom typeDockAtom = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_DOCK", 0);
  XChangeProperty(dpy, window, typeAtom, XA_ATOM, 32, PropModeReplace,
                  (unsigned char*)&typeDockAtom, 1);

  // We're setting struts to try to keep other windows out of our way, but if
  // the user really wants to move a window over us, we should err on the side
  // of discretion, and not get in their way. After all, we reside at the top
  // of the screen, so forcing ourselves on top (which is the usual default
  // for 'dock' windows) is more likely to get in the way of the user's ability
  // to move a window out of the way.
  Atom stateAtom = XInternAtom(dpy, "_NET_WM_STATE", 0);
  Atom stateBelowAtom = XInternAtom(dpy, "_NET_WM_STATE_BELOW", 0);
  XChangeProperty(dpy, window, stateAtom, XA_ATOM, 32, PropModeReplace,
                  (unsigned char*)&stateBelowAtom, 1);

  unsigned long val[12] = {};
  // _NET_WM_STRUT provides left, right, top, bottom. As our window only appears
  // at the top, we only set top to the height of the window.
  val[2] = height;  // top
  setWindowProperty(window, "_NET_WM_STRUT", val, 4);
  // _NET_WM_STRUT_PARTIAL starts the same as _NET_WM_STRUT, and then has a
  // pair of 'start' and 'end' values per screen edge. The ones we're interested
  // in are top_start_x and top_end_x, which are at indices 8 and 9.
  val[8] = x;          // top_start_x
  val[9] = x + width;  // top_end_x
  setWindowProperty(window, "_NET_WM_STRUT_PARTIAL", val, 4);
}

// XFreer collects pointers that need to be freed using XFree, and then deletes
// them all when it is itself destroyed.
// It is safe to use it with null pointers (it won't call XFree for them).
class XFreer {
 public:
  XFreer() = default;
  ~XFreer() {
    for (void* v : victims_) {
      XFree(v);
    }
  }

  template <typename T>
  T Later(T t) {
    add((void*)t);
    return t;
  }

 private:
  void add(void* v) {
    if (v) {
      victims_.push_back(v);
    }
  }

  std::vector<void*> victims_;
};

static void setSizeFromXRandR() {
  XFreer xfree;
  XRRScreenResources* res =
      xfree.Later(XRRGetScreenResourcesCurrent(dpy, DefaultRootWindow(dpy)));
  if (!res) {
    LOGE() << "Failed to get XRRScreenResources";
    return;
  }
  if (!res->ncrtc) {
    LOGE() << "Empty list of CRTs";
    return;
  }
  // Ignore any CRT with mode==0.
  // We always want to be displayed at the top of the topmost screen. So go
  // through the screens, and collect together all X ranges with ymin==0.
  std::map<int, int> x_ranges;
  for (int i = 0; i < res->ncrtc; i++) {
    XRRCrtcInfo* crt = xfree.Later(XRRGetCrtcInfo(dpy, res, res->crtcs[i]));
    if (!crt->mode) {
      continue;
    }
    if (crt->y != 0) {
      continue;
    }
    x_ranges[crt->x] = crt->x + crt->width;
  }
  // Probably we just want the leftmost stretch of screen width, but we'll
  // stitch multiple screens together if we can.
  // Another possible choice would be to find the widest area.
  if (x_ranges.empty()) {
    LOGE() << "Xrandr reported no screen space at y=0";
    return;
  }
  int min = x_ranges.begin()->first;
  int max = 0;
  for (int v = x_ranges[min]; v; v = x_ranges[v]) {
    max = v;
  }
  display_width = max - min;
  XMoveResizeWindow(dpy, window, min, 0, display_width, window_height);
}

static void rrScreenChangeNotify(XEvent* ev) {
  XRRScreenChangeNotifyEvent* rrev = (XRRScreenChangeNotifyEvent*)ev;
  static long lastSerial;
  if (rrev->serial == lastSerial) {
    LOGI() << "Dropping duplicate event for serial " << std::hex << lastSerial;
    return;  // Drop duplicate message (we get lots of these).
  }
  lastSerial = rrev->serial;
  setSizeFromXRandR();
}

int main(int argc, char* argv[]) {
  struct sigaction sa = {};
  sa.sa_handler = RestartSelf;
  sigaddset(&(sa.sa_mask), SIGHUP);
  if (sigaction(SIGHUP, &sa, NULL)) {
    LOGF() << "SIGHUP sigaction failed: " << Log::Errno(errno);
  }

  // Open a connection to the X server.
  dpy = XOpenDisplay("");
  LOGF_IF(!dpy) << "can't open display";

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
    LOGE() << "couldn't find font " << g_font_name << "; trying default";
    g_font = XftFontOpenName(dpy, 0, "fixed");
    LOGF_IF(g_font == nullptr) << "can't find a font";
  }

  // Create the window.
  window_height = 1.2 * (g_font->ascent + g_font->descent);
  root = DefaultRootWindow(dpy);
  XSetWindowAttributes attr;
  attr.override_redirect = False;
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

  // Set struts on the window, so the window manager knows where not to place
  // other windows.
  setWindowProps(window, 0, 0, display_width, window_height);

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

  // Do we need to support XRandR?
  int rr_event_base, rr_error_base;
  bool have_rr = XRRQueryExtension(dpy, &rr_event_base, &rr_error_base);
  if (have_rr) {
    XRRSelectInput(dpy, root, RRScreenChangeNotifyMask);
    setSizeFromXRandR();
  }

  // Bring up the window.
  XMapRaised(dpy, window);

  // Make sure all our communication to the server got through.
  XSync(dpy, False);

  // The main event loop.
  while (!forceRestart) {
    XEvent ev;
    getEvent(&ev);
    if (ev.type == rr_event_base + RRScreenChangeNotify) {
      rrScreenChangeNotify(&ev);
      continue;
    }
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
