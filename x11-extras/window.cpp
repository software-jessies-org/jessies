#include <errno.h>
#include <error.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>

#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#include <X11/X.h>
#include <X11/Xatom.h>
#include <X11/Xlib.h>
#include <X11/Xos.h>
#include <X11/Xutil.h>

struct ReasonDesc {
  const char* name;
  int nargs;
  void (*fn)(char**);
  const char* usage;
};

static Display* dpy;

static Window wind(char* p) {
  if (!strcmp(p, "root")) {
    return DefaultRootWindow(dpy);
  }

  char* endp;
  errno = 0;
  long l = strtol(p, &endp, 0);
  if (p == endp || errno != 0 || *endp != '\0') {
    error(1, 0, "%s is not a valid window id", p);
  }
  return (Window)l;
}

static int listwindows(Window w, Window** ws) {
  unsigned nkids;
  Window dummy;
  XQueryTree(dpy, w, &dummy, &dummy, ws, &nkids);
  return nkids;
}

static void RaiseWindow(char* argv[]) {
  XRaiseWindow(dpy, wind(*argv));
}

static void LowerWindow(char* argv[]) {
  XLowerWindow(dpy, wind(*argv));
}

static void KillWindow(char* argv[]) {
  XKillClient(dpy, wind(*argv));
}

static void HideWindow(char* argv[]) {
  Window root = DefaultRootWindow(dpy);
  Atom wm_change_state = XInternAtom(dpy, "WM_CHANGE_STATE", False);

  XEvent ev = {};
  ev.xclient.type = ClientMessage;
  ev.xclient.window = wind(*argv);
  ev.xclient.message_type = wm_change_state;
  ev.xclient.format = 32;
  ev.xclient.data.l[0] = IconicState;
  ev.xclient.data.l[1] = CurrentTime;

  XSendEvent(dpy, root, False, SubstructureRedirectMask, &ev);
}

static void UnhideWindow(char* argv[]) {
  XMapWindow(dpy, wind(*argv));
}

static void printwindowname(Window w) {
  unsigned char* name;
  Atom actual_type;
  int format;
  unsigned long n;
  unsigned long extra;

  /*
   *      This rather unpleasant hack is necessary because xwsh uses
   *      COMPOUND_TEXT rather than STRING for its WM_NAME property,
   *      and anonymous xwsh windows are annoying.
   */
  if (Success == XGetWindowProperty(dpy, w, XA_WM_NAME, 0L, 100L, False,
                                    AnyPropertyType, &actual_type, &format, &n,
                                    &extra, &name) &&
      name != 0) {
    printf("%#x\t%s\n", (unsigned)w, (char*)name);
  } else {
    printf("%#x\t%s\n", (unsigned)w, "(none)");
  }

  XFree(name);
}

static void LabelWindow(char* argv[]) {
  Window w = wind(argv[0]);
  XStoreName(dpy, w, argv[1]);
  XSetIconName(dpy, w, argv[1]);
}

/*
 *    This can only give us an approximate geometry since
 *    we can't find out what decoration the window has.
 */
static void WhereWindow(char* argv[]) {
  Window root;
  int x, y;
  unsigned w, h, border, depth;
  XGetGeometry(dpy, wind(*argv), &root, &x, &y, &w, &h, &border, &depth);
  printf("-geometry %ux%u+%u+%u\n", w, h, x, y);
}

static void MoveWindow(char* argv[]) {
  XMoveWindow(dpy, wind(argv[0]), atoi(argv[1]), atoi(argv[2]));
}

static void ResizeWindow(char* argv[]) {
  XResizeWindow(dpy, wind(argv[0]), atoi(argv[1]), atoi(argv[2]));
}

static void ListWindows(char* argv[]) {
  Window* kids;
  unsigned nkids = listwindows(DefaultRootWindow(dpy), &kids);

  for (unsigned i = 0; i < nkids; i++) {
    Window* kids2;
    unsigned nkids2 = listwindows(kids[i], &kids2);

    for (unsigned i2 = 0; i2 < nkids2; i2++) {
      XWindowAttributes attr;
      XGetWindowAttributes(dpy, kids2[i2], &attr);
      if (attr.override_redirect == 0 && attr.map_state == IsViewable)
        printwindowname(kids2[i2]);
    }

    XFree(kids2);
  }

  XFree(kids);
}

static void getWindowProperty(Window window,
                              const char* name,
                              Bool delete_afterwards) {
  Atom prop = XInternAtom(dpy, name, True);
  if (prop == None) {
    error(0, 0, "no such property '%s'", name);
    return;
  }

  Atom realType;
  int format;
  unsigned long n;
  unsigned long extra;
  unsigned char* value;
  int status = XGetWindowProperty(dpy, window, prop, 0L, 512L,
                                  delete_afterwards, AnyPropertyType, &realType,
                                  &format, &n, &extra, (unsigned char**)&value);
  if (status != Success || value == 0 || *value == 0 || n == 0) {
    error(0, 0, "couldn't read property on window %lx", window);
    return;
  }

  printf("%s\n", value);
}

static void GetProperty(char* argv[]) {
  getWindowProperty(wind(argv[0]), argv[1], False);
}

static void SetProperty(char* argv[]) {
  Atom prop;
  char* name = argv[1];
  char* value = argv[2];

  prop = XInternAtom(dpy, name, True);
  if (prop == None) {
    error(0, 0, "no such property '%s'\n", name);
    return;
  }

  XChangeProperty(dpy, wind(argv[0]), prop, XA_STRING, 8, PropModeReplace,
                  (unsigned char*)value, strlen(value));
}

static void WarpPointer(char* argv[]) {
  XWarpPointer(dpy, None, wind(argv[0]), 0, 0, 0, 0, atoi(argv[1]),
               atoi(argv[2]));
}

static void GetFocusWindow(char* argv[]) {
  Window focusReturn;
  int revertToReturn;
  (void)argv;
  XGetInputFocus(dpy, &focusReturn, &revertToReturn);
  printwindowname(focusReturn);
}

static void GetSelection(char* argv[]) {
  // Create an unmapped invisible window.
  Window window =
      XCreateWindow(dpy, DefaultRootWindow(dpy), 0, 0, 1, 1, 0, CopyFromParent,
                    InputOnly, CopyFromParent, 0, 0);

  // Ask that the current selection be placed in a property on the window.
  Atom dataProperty = XInternAtom(dpy, "SELECTION", False);
  XConvertSelection(dpy, XA_PRIMARY, XA_STRING, dataProperty, window,
                    CurrentTime);

  // Wait for it to arrive.
  XEvent ev;
  do {
    XNextEvent(dpy, &ev);
  } while (ev.type != SelectionNotify);

  getWindowProperty(window, "SELECTION", True);
}

static void DeleteSelection(char* argv[]) {
  XSetSelectionOwner(dpy, XA_PRIMARY, None, CurrentTime);
}

static void CirculateUp(char* argv[]) {
  XCirculateSubwindowsUp(dpy, DefaultRootWindow(dpy));
}

static void CirculateDown(char* argv[]) {
  XCirculateSubwindowsDown(dpy, DefaultRootWindow(dpy));
}

static ReasonDesc reasons[] = {
    {"-move", 3, MoveWindow, "<id> <x> <y>"},
    {"-resize", 3, ResizeWindow, "<id> <x> <y>"},
    {"-raise", 1, RaiseWindow, "<id>"},
    {"-label", 2, LabelWindow, "<id> <text>"},
    {"-lower", 1, LowerWindow, "<id>"},
    {"-kill", 1, KillWindow, "<id>"},
    {"-hide", 1, HideWindow, "<id>"},
    {"-unhide", 1, UnhideWindow, "<id>"},
    {"-where", 1, WhereWindow, "<id>"},
    {"-list", 0, ListWindows, ""},
    {"-getprop", 2, GetProperty, "<id> <name>"},
    {"-setprop", 3, SetProperty, "<id> <name> <value>"},
    {"-warppointer", 3, WarpPointer, "<id> <x> <y>"},
    {"-getfocuswindow", 0, GetFocusWindow, ""},
    {"-getsel", 0, GetSelection, ""},
    {"-delsel", 0, DeleteSelection, ""},
    {"-circup", 0, CirculateUp, ""},
    {"-circdown", 0, CirculateDown, ""},
};

static int handler(Display* disp, XErrorEvent* err) {
  error(1, 0, "no window with id %#lx", err->resourceid);
  return 0;
}

static void usage() {
  fprintf(stderr, "usage:\n");
  for (ReasonDesc* p = reasons;
       p < reasons + sizeof reasons / sizeof reasons[0]; p++) {
    fprintf(stderr, "\twindow %s %s\n", p->name, p->usage);
  }
}

extern int main(int argc, char* argv[]) {
  dpy = XOpenDisplay("");
  if (dpy == nullptr) {
    error(1, 0, "can't open display");
  }

  XSetErrorHandler(handler);

  if (argc > 1) {
    for (ReasonDesc* p = reasons;
         p < reasons + sizeof reasons / sizeof reasons[0]; p++) {
      if (strcmp(p->name, argv[1]) == 0) {
        if (argc - 2 != p->nargs) {
          error(1, 0, "the %s option requires %d argument%s", argv[1], p->nargs,
                p->nargs > 1 ? "s" : "");
        }
        p->fn(argv + 2);
        XSync(dpy, True);
        return EXIT_SUCCESS;
      }
    }
  }

  usage();
  return EXIT_FAILURE;
}
