// gummiband is a launcher.
//
// The config file is called '.gummiband', and consists of a sequence of
// entries, each representing an item on the menu. Each entry is defined by a
// sequence of key=value pairs.
//
// A few examples:
// # A simple item on the left side, named 'XTerm', which runs xterm:
// name=XTerm
// click=exec /usr/local/bin/xterm
//
// # A clock on the right-hand side, which will update every second.
// name=exec date +"%Y-%m-%d %H:%M:%S"
// position=right
// updatesecs=1
//
// # A drop-down menu on the right-hand side, which displays which audio
// # connector is active, and offers a means to switch.
// # Note: the 'audioconn' command is invented; I imagine you might want to
// # write a script and put it in $HOME/bin/.
// # Only 'front' and 'back' are valid values here; whichever is selected will
// # be passed to the 'exec audioconn', being substituted in for '<item>'.
// name=exec audioconn status
// updatesecs=5
// position=right
// menuitems=front,back
// menuclick=exec audioconn <item>
//
// # A drop-down menu on the left-hand side, which displays the set of network
// # interfaces available. We assume to have a 'networkconn' script which
// # when called with the argument 'status' prints out the current network
// # status, when called with 'list' prints out the set of network interfaces
// # (one per line), and when called with any other string, it tries to connect
// # to that network.
// name=exec networkconn status
// updatesecs=10
// position=left
// menuitems=exec networkconn list
// menuclick=exec networkconn <item>
//
// End of examples.
//
// In general, if a value begins with 'exec ', it will be treated as a command
// to execute, and anything printed out by that command on stdout will be used
// as the value. For the name, for example, the following entries would look
// the same (although one's more costly than the other):
//
// name=hello
// name=exec echo hello
//
// The keyword 'name=' always begins a new item. Anything following that name
// will be attributed to it.
//
// Entries added to the left will be added left to right (so the first is the
// leftmost); entries added to the right are added right to left (so the first
// is the rightmost).

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

using namespace std;

#define NullEvent -1

// Only set this to non-zero if you're running a test gummiband alongside your
// normal instance.
#define Y_OFFSET 0

// The following are the Xresource values we read. They are:
// The font to use (default: roboto-16)
#define XRES_FONT "gummiband.font"
// The normal background colour (default: white)
#define XRES_BG "gummiband.background"
// The normal text foreground colour (default: black)
#define XRES_FG "gummiband.foreground"
// The background colour for the pointed-at item (default: pale blue).
#define XRES_SEL_BG "gummiband.selBackground"
// The text foreground colour for the pointed-at item (default: black).
#define XRES_SEL_FG "gummiband.selForeground"

// The connection to the X server.
static Display* dpy;

// display_xmin can be non-zero, if we'rd using xrandr and the highest screen
// is offset from the X=0 line. This happens, for example, if I have my laptop
// connected to an external screen, in the following configuration (note - the
// '=' signs shows where gummiband's window is positioned):
//
//   display_xmin
//    |
// <----->
//        +==========+
//        |          |
// +------|          |
// |      |          |
// |______|__________|
static int display_xmin;
static int display_width;

static Window window;           // Main window.
static Window dropdown_window;  // Drop-down window.
static int window_height;
static GC dropdown_gc;
static GC dropdown_highlight_gc;

// Font stuff.
static XftFont* g_font;
static XftDraw* g_dropdown_font_draw;
static XftColor g_font_color;
static XftColor g_selected_font_color;
static int g_font_height;
static int g_font_yoff;

// Colours as pulled out from the Xresources.
static unsigned long colour_bg;
static unsigned long colour_fg;
static unsigned long colour_sel_bg;
static unsigned long colour_sel_fg;

class Menu;
class MenuItem;
class Updaters;
class DropDown;

static Menu* menu;
static MenuItem* selected;
static Updaters* updaters;
static DropDown* dropdown;

static bool forceRestart;

// Split does the obvious. Eg Split("a; b; c", "; ") -> ["a", "b", "c"].
static vector<string> Split(const string& in, const string& sep) {
  size_t begin = 0;
  vector<string> res;
  while (begin < in.size()) {
    const size_t end = in.find(sep, begin);
    if (end == string::npos) {
      res.push_back(in.substr(begin));
      return res;
    }
    res.push_back(in.substr(begin, end - begin));
    begin = end + sep.size();
  }
  return res;
}

// KV returns the key and value (key=val) as a pair.
static pair<string, string> KV(const string& in) {
  int i = in.find('=');
  if (i == string::npos) {
    return make_pair(in, string());
  }
  return make_pair(in.substr(0, i), in.substr(i + 1));
}

// kDefaultConfig is the default configuration: an xterm on the left side, and
// a clock on the right. See the man page, or the comment at the top of this
// file, for more examples on how to customise this.
const char* kDefaultConfig = R"CFG(
name=XTerm
click=exec xterm

name=exec date +"%Y-%m-%d %H:%M:%S"
position=right
updatesecs=1
)CFG";

// LoadConfigLines tries to load in all lines from a file called '.gummiband',
// or if that's not there, returns kDefaultConfig, split into lines.
static vector<string> LoadConfigLines() {
  FILE* fp = fopen(".gummiband", "r");
  if (!fp) {
    return Split(kDefaultConfig, "\n");
  }
  char line[BUFSIZ];
  vector<string> res;
  while (fgets(line, BUFSIZ, fp) != 0) {
    int len = strlen(line);
    while (len > 0 && (line[len - 1] == '\r' || line[len - 1] == '\n')) {
      len--;
    }
    res.push_back(string(line, len));
  }
  fclose(fp);
  return res;
}

// StringsSource abstracts away where we're getting strings from - by a static
// list, or by executing some sub-process and reading its stdout.
class StringsSource {
 public:
  virtual ~StringsSource() = default;
  virtual vector<string> Get() = 0;
};

// StringSource adapts the list-containing StringsSource to return a single
// string, with multiple values separated by spaces.
class StringSource {
 public:
  explicit StringSource(StringsSource* ss) : ss_(ss) {}
  string Get() {
    string res;
    vector<string> vals = ss_->Get();
    for (int i = 0; i < vals.size(); i++) {
      if (i > 0) {
        res += " ";
      }
      res += vals[i];
    }
    return res;
  }

 private:
  StringsSource* ss_;
};

// StaticStringsSource is a StringsSource that has a pre-defined set of
// strings it returns every time.
class StaticStringsSource : public StringsSource {
 public:
  explicit StaticStringsSource(const string& s) : strings_(1, s) {}
  explicit StaticStringsSource(const vector<string>& ss) : strings_(ss) {}
  vector<string> Get() { return strings_; }

 private:
  vector<string> strings_;
};

// TrimTrailingWhitespace removes all newlines, tabs, spaces etc from the right-
// -hand side of the string.
static string TrimTrailingWhitespace(const string& s) {
  const int pos = s.find_last_not_of("\t\n\r ");
  if (pos == string::npos) {
    return s;
  }
  return s.substr(0, pos + 1);
}

// ExecStringsSource returns stdout from the run command, one entry per line.
// The Get() function will block until the sub-process completes.
class ExecStringsSource : public StringsSource {
 public:
  explicit ExecStringsSource(const string& command) : command_(command) {}

  vector<string> Get() {
    vector<string> res;
    FILE* fp = popen(command_.c_str(), "r");
    if (fp == NULL) {
      LOGE() << "Failed to run command '" << command_ << "'";
      return res;
    }
    char line[BUFSIZ];
    while (fgets(line, BUFSIZ, fp) != 0) {
      res.push_back(TrimTrailingWhitespace(line));
    }
    pclose(fp);
    return res;
  }

 private:
  const string command_;
};

// UpdatableStringsSource is backed by an ExecStringsSource, but caches the
// result, only refreshing when its update timer expires. Normally this will be
// registered with the 'Updaters' instance to ensure any cached data is updated
// regularly.
class UpdatableStringsSource : public StringsSource {
 public:
  UpdatableStringsSource(ExecStringsSource* source, int update_secs)
      : source_(source), update_secs_(update_secs), last_update_(0) {}

  vector<string> Get() {
    Update();  // Just in case.
    return cached_;
  }

  time_t NextUpdateTime() { return last_update_ + update_secs_; }

  // Returns true if the source was update, and that update yielded a new value.
  bool Update() {
    time_t now = time(nullptr);
    // The check for now >= last_update_ is to cope with the clock going
    // backwards, eg. when the computer's clock was messed up and ntpd wakes up
    // and fixes it.
    if (now >= last_update_ && now < NextUpdateTime()) {
      return false;
    }
    vector<string> new_val = source_->Get();
    last_update_ = now;
    if (new_val == cached_) {
      return false;
    }
    cached_ = new_val;
    return true;
  }

 private:
  ExecStringsSource* source_;
  vector<string> cached_;
  int update_secs_;
  time_t last_update_;
};

// Updaters holds onto all the things that need to be updated on some schedule,
// and calls their Update functions when appropriate.
class Updaters {
 public:
  Updaters() = default;

  void Add(UpdatableStringsSource* uss) { upds_.push_back(uss); }

  // Updates anything that needs it, returning true if any yielded a new value.
  bool Update() {
    bool res = false;
    for (UpdatableStringsSource* uss : upds_) {
      res |= uss->Update();
    }
    return res;
  }

 private:
  // We just keep the updatable things in whatever order. We could turn this
  // into a priority queue, but it's not worth the effort.
  vector<UpdatableStringsSource*> upds_;
};

// Action is a generic 'thing that can happen if you click on a MenuItem'.
class Action {
 public:
  virtual ~Action() = default;
  // x, y and width denote the line spanning the bottom of the clicked menu
  // item. This can be used if a drop-down menu is to be created, to position
  // it correctly.
  virtual void Act(int x, int y, int width) = 0;
};

// Exec runs the given command in the background as a child process, but returns
// immediately without waiting for the child to complete. This function is used
// for all actual launching.
static void Exec(const string& command) {
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
          execl(sh, sh, "-c", command.c_str(), (char*)NULL);
          LOGF() << "exec \"" << sh << " -c " << command.c_str()
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

// ExecAction is an action attached to a MenuItem whose sole purpose is to run
// a fixed command when clicked. This is the thing that supports your 'Xterm'
// button.
class ExecAction : public Action {
 public:
  explicit ExecAction(const string& command) : command_(command) {}
  void Act(int, int, int) { Exec(command_); }

 private:
  string command_;
};

// TextWidth returns the display width of the given string in pixels.
static int TextWidth(const string& s) {
  XGlyphInfo extents;
  XftTextExtentsUtf8(dpy, g_font, reinterpret_cast<const FcChar8*>(s.data()),
                     s.size(), &extents);
  return extents.xOff;
}

// FontColour returns the foreground font colour for normal or highlighted text.
static XftColor* FontColour(bool selected) {
  return selected ? &g_selected_font_color : &g_font_color;
}

// A DropDown instance is created when the drop-down menu is opened from some
// MenuItem. It holds the set of items that were queried when the drop-down
// menu opened, handles drawing the menu, and acts on any click on a drop-down
// item.
class DropDown {
 public:
  DropDown(int width, const vector<string>& items, const string& command)
      : width_(width), items_(items), command_(command), selected_index_(-1) {}

  void Close() {
    XUnmapWindow(dpy, dropdown_window);
    delete this;
    dropdown = nullptr;
  }

  void Paint() {
    XClearWindow(dpy, dropdown_window);
    const int y_max = items_.size() * g_font_height - 1;
    const int x_max = width_ - 1;
    XDrawLine(dpy, dropdown_window, dropdown_gc, 0, 0, 0, y_max);
    XDrawLine(dpy, dropdown_window, dropdown_gc, 0, y_max, x_max, y_max);
    XDrawLine(dpy, dropdown_window, dropdown_gc, x_max, 0, x_max, y_max);
    for (int i = 0; i < items_.size(); i++) {
      const int y = i * g_font_height;
      const string& item = items_[i];
      if (i == selected_index_) {
        XFillRectangle(dpy, dropdown_window, dropdown_highlight_gc, 5, y,
                       width_ - 10, g_font_height);
      }
      XftDrawStringUtf8(g_dropdown_font_draw, FontColour(i == selected_index_),
                        g_font, 10, y + g_font_yoff,
                        reinterpret_cast<const FcChar8*>(item.data()),
                        item.size());
    }
  }

  void MouseMove(int x, int y) {
    const int index = GetIndexAt(x, y);
    if (index == selected_index_) {
      return;
    }
    selected_index_ = index;
    Paint();
  }

  void MouseLeft() {
    const bool was_selected = selected_index_ != -1;
    selected_index_ = -1;
    if (was_selected) {
      Paint();
    }
  }

  void MouseClick(int x, int y) {
    const int index = GetIndexAt(x, y);
    if (index == -1) {
      Close();
      return;
    }
    string command = command_;
    const size_t i = command_.find("<item>");
    if (i != string::npos) {
      command = command_.substr(0, i) + items_[index] + command_.substr(i + 6);
    }
    Exec(command);
    Close();
    return;
  }

 private:
  const int width_;
  const vector<string> items_;
  const string command_;
  int selected_index_;

  int GetIndexAt(int x, int y) {
    if (x < 0 || x >= width_ || y < 0 || y >= g_font_height * items_.size()) {
      return -1;
    }
    return y / g_font_height;
  }
};

// DropDownMenuAction acts on a click on a MenuItem by opening a drop-down
// menu at the appropriate position. It also creates a 'DropDown' instance to
// handle the menu while it's open.
class DropDownMenuAction : public Action {
 public:
  DropDownMenuAction(StringsSource* items, const string& command)
      : items_(items), command_(command) {}

  void Act(int x, int y, int width) {
    if (dropdown) {
      dropdown->Close();
      return;
    }
    vector<string> items = items_->Get();
    if (items.empty()) {
      items.push_back("<empty>");
    }
    const int height = items.size() * g_font_height;
    for (const string& item : items) {
      const int iw = TextWidth(item) + 20;
      if (iw > width) {
        width = iw;
      }
    }
    if (x + width > display_width) {
      x = display_width - width;
    }
    // So far, x is relative to gummiband's main window coordinates, but those
    // migth be offset, for example if we have multiple screens and the highest
    // (the one with Y=0) as an min X location greater than 0. See the comment
    // on the definition of 'display_xmin' above for a diagram.
    // Opening a window has to happen relative to 0, 0 in the overall display
    // coordinates. So we must apply the display_xmin so it appears at the right
    // location.
    x += display_xmin;
    XMoveResizeWindow(dpy, dropdown_window, x, y + Y_OFFSET, width, height);
    XMapRaised(dpy, dropdown_window);
    dropdown = new DropDown(width, items, command_);
  }

 private:
  StringsSource* items_;
  const string command_;
};

// MenuItem is a single entity on the gummiband. It might or might not be
// clickable; clicking it may execute something or create a drop-down menu,
// and its text may be fixed or dynamic.
// Note that for dynamic text, we rely on the underlying StringSource to sort
// that out - MenuItem itself has no concept of updating things.
class MenuItem {
 public:
  MenuItem(StringSource* name_source, Action* action)
      : name_source_(name_source), action_(action) {}

  string Name() { return name_source_->Get(); }

  bool HasAction() { return action_; }

  void Act() {
    if (action_) {
      action_->Act(x_, window_height, width_);
    }
  }

  // Called by the redraw code to let the menu item know where it's being drawn.
  void SetPosition(int x, int width) {
    x_ = x;
    width_ = width;
  }

  bool ContainsX(int x) { return x >= x_ && x < x_ + width_; }

 private:
  StringSource* name_source_;
  Action* action_;
  int x_;
  int width_;
};

// MenuSet holds a set of menu items, in order. There's one MenuSet for the
// left-hand set, and one for the right-hand set of items.
class MenuSet {
 public:
  explicit MenuSet(bool rtl) : rtl_(rtl){};
  ~MenuSet() = default;

  // Takes ownership of item.
  void Add(MenuItem* item) { items_.push_back(item); }

  void Paint(Drawable target, GC highlight_gc, XftDraw* g_font_draw) {
    int x = rtl_ ? (display_width - 5) : 5;
    for (MenuItem* mi : items_) {
      const string name = mi->Name();
      const int width = 20 + TextWidth(name);
      if (rtl_) {
        x -= width;
      }
      mi->SetPosition(x, width);
      if (mi == selected) {
        XFillRectangle(dpy, target, highlight_gc, x + 5, 0, width - 10,
                       window_height);
      }
      XftDrawStringUtf8(
          g_font_draw, FontColour(mi == selected), g_font, x + 10, g_font_yoff,
          reinterpret_cast<const FcChar8*>(name.data()), name.size());
      if (!rtl_) {
        x += width;
      }
    }
  }

  MenuItem* ItemAt(int mouseX) {
    for (MenuItem* mi : items_) {
      if (mi->ContainsX(mouseX) && mi->HasAction()) {
        return mi;
      }
    }
    return nullptr;
  }

 private:
  bool rtl_;
  vector<MenuItem*> items_;
};

// Menu is the thing that has the sets of left and right menu items.
class Menu {
 public:
  Menu()
      : left_(new MenuSet(false)), right_(new MenuSet(true)), buffer_(None) {}

  void Add(MenuItem* item, bool is_right) {
    (is_right ? right_ : left_)->Add(item);
  }

  void Paint() {
    EnsureDrawBufferExists();
    XFillRectangle(dpy, buffer_, clear_gc_, 0, 0, display_width, window_height);
    left_->Paint(buffer_, highlight_gc_, g_font_draw_);
    right_->Paint(buffer_, highlight_gc_, g_font_draw_);
    XCopyArea(dpy, buffer_, window, cp_gc_, 0, 0, display_width, window_height,
              0, 0);
  }

  MenuItem* ItemAt(int mouseX) {
    MenuItem* res = left_->ItemAt(mouseX);
    return res ? res : right_->ItemAt(mouseX);
  }

  void SizeChanged() {
    if (buffer_ != None) {
      XFreePixmap(dpy, buffer_);
      XFreeGC(dpy, clear_gc_);
      XFreeGC(dpy, highlight_gc_);
      XFreeGC(dpy, cp_gc_);
      buffer_ = None;
    }
  }

 private:
  void EnsureDrawBufferExists() {
    if (buffer_ != None) {
      return;
    }
    const auto screen = DefaultScreen(dpy);
    const auto depth = DefaultDepth(dpy, screen);
    buffer_ = XCreatePixmap(dpy, window, display_width, window_height, depth);
    {
      XGCValues gv;
      gv.foreground = colour_bg;
      clear_gc_ = XCreateGC(dpy, buffer_, GCForeground, &gv);
      gv.foreground = colour_sel_bg;
      highlight_gc_ = XCreateGC(dpy, buffer_, GCForeground, &gv);
    }
    {
      XGCValues gv;
      gv.function = GXcopy;
      cp_gc_ = XCreateGC(dpy, window, GCFunction, &gv);
    }
    g_font_draw_ = XftDrawCreate(dpy, buffer_, DefaultVisual(dpy, screen),
                                 DefaultColormap(dpy, screen));
  }

  MenuSet* left_;
  MenuSet* right_;

  Pixmap buffer_;
  GC clear_gc_;
  GC highlight_gc_;
  GC cp_gc_;
  XftDraw* g_font_draw_;
};

static void getEvent(XEvent*);

static bool IsExec(const string& s) {
  return (s.size() > 5) && (s.substr(0, 5) == "exec ");
}

struct ItemState {
  explicit ItemState(int line)
      : start_line(line), update_secs(0), is_right(false) {}

  int start_line;
  string name;
  int update_secs;
  bool is_right;
  string click;
  string menu_items;
  string menu_click;

  void CreateItem() {
    if (name.empty()) {
      return;
    }
    StringsSource* name_src;
    if (IsExec(name)) {
      ExecStringsSource* ess = new ExecStringsSource(name);
      if (update_secs > 0) {
        UpdatableStringsSource* uss =
            new UpdatableStringsSource(ess, update_secs);
        updaters->Add(uss);
        name_src = uss;
      } else {
        name_src = ess;
      }
    } else {
      name_src = new StaticStringsSource(name);
    }
    Action* action = nullptr;
    if (!click.empty()) {
      action = new ExecAction(click);
    } else if (!menu_items.empty()) {
      StringsSource* items = nullptr;
      if (IsExec(menu_items)) {
        items = new ExecStringsSource(menu_items);
      } else {
        items = new StaticStringsSource(Split(menu_items, ","));
      }
      if (menu_click.empty()) {
        LOGF() << Context() << "Menu with no 'menuclick' command";
      } else if (menu_click.find("<item>") == string::npos) {
        LOGF() << Context() << "menuclick command '" << menu_click
               << "' does not contain substring '<item>'";
      }
      action = new DropDownMenuAction(items, menu_click);
    }
    MenuItem* item = new MenuItem(new StringSource(name_src), action);
    menu->Add(item, is_right);
  }

  string Context() {
    ostringstream str;
    str << "Item at line " << start_line << ": ";
    return str.str();
  }
};

static bool IgnoreConfigLine(const string& line) {
  return line.empty() || line[0] == '#';
}

static int ParsePositiveInt(const string& s) {
  const int val = (int)strtol(s.c_str(), (char**)0, 0);
  if (errno == EINVAL) {
    LOGE() << "Failed to parse int '" << s << "'";
  }
  return (errno == EINVAL) ? 0 : val;
}

static void ReadConfig() {
  const vector<string> lines = LoadConfigLines();
  menu = new Menu;
  updaters = new Updaters;

  ItemState state(0);
  for (int i = 0; i < lines.size(); i++) {
    if (IgnoreConfigLine(lines[i])) {
      continue;
    }
    pair<string, string> kv = KV(lines[i]);
    if (kv.first == "name") {
      state.CreateItem();
      state = ItemState(i);
      state.name = kv.second;
    } else if (kv.first == "updatesecs") {
      const int val = (int)strtol(kv.second.c_str(), (char**)0, 0);
      if (errno == EINVAL) {
        LOGE() << "line " << (i + 1) << ": can't parse int '" << kv.second
               << "'";
      } else if (val < 0) {
        LOGE() << "line " << (i + 1) << ": updatesecs must be positive";
      } else {
        state.update_secs = val;
      }
    } else if (kv.first == "position") {
      if (kv.second != "left" && kv.second != "right") {
        LOGE() << "line " << (i + 1) << ": position must be left or right";
      } else {
        state.is_right = kv.second == "right";
      }
    } else if (kv.first == "click") {
      state.click = kv.second;
    } else if (kv.first == "menuitems") {
      state.menu_items = kv.second;
    } else if (kv.first == "menuclick") {
      state.menu_click = kv.second;
    }
  }
  state.CreateItem();
}

static void DoExpose(XEvent* ev) {
  // Only handle the last in a group of Expose events.
  if (ev && ev->xexpose.count != 0) {
    return;
  }
  menu->Paint();
  if (dropdown) {
    dropdown->Paint();
  }
}

static void DoNullEvent(XEvent* ev) {
  DoExpose(nullptr);
}

static void DoMouseMoved(XEvent* ev) {
  if (ev->xmotion.window == dropdown_window && dropdown) {
    dropdown->MouseMove(ev->xbutton.x, ev->xbutton.y);
    return;
  }
  while (XCheckMaskEvent(dpy, ButtonMotionMask, ev)) {
  }

  if (!XQueryPointer(dpy, ev->xmotion.window, &(ev->xmotion.root),
                     &(ev->xmotion.subwindow), &(ev->xmotion.x),
                     &(ev->xmotion.y), &(ev->xmotion.x), &(ev->xmotion.y),
                     &(ev->xmotion.state))) {
    return;
  }
  MenuItem* old_selected = selected;
  selected = menu->ItemAt(ev->xmotion.x);
  if (old_selected != selected) {
    DoExpose(nullptr);
  }
}

static void DoButtonPress(XEvent* ev) {
  if (ev->xmotion.window == dropdown_window && dropdown) {
    dropdown->MouseClick(ev->xbutton.x, ev->xbutton.y);
    return;
  }
  selected = menu->ItemAt(ev->xbutton.x);
  DoExpose(nullptr);
}

static void DoButtonRelease(XEvent* ev) {
  if (selected != 0) {
    selected->Act();
  } else if (dropdown) {
    dropdown->Close();
  }
  DoExpose(nullptr);
}

static void DoLeave(XEvent* ev) {
  if (ev->xcrossing.window == dropdown_window && dropdown) {
    dropdown->MouseLeft();
  } else {
    selected = nullptr;
    DoExpose(nullptr);
  }
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

static void AddResource(map<string, string>* target,
                        XrmDatabase db,
                        const string& name,
                        const string& want_type,
                        const string& dflt) {
  (*target)[name] = dflt;
  if (!db) {
    return;
  }
  char* type;
  XrmValue value;
  if (!XrmGetResource(db, name.c_str(), want_type.c_str(), &type, &value)) {
    return;
  }
  if (strcmp(type, "String")) {
    return;
  }
  (*target)[name] = value.addr;
}

extern map<string, string> GetResources() {
  map<string, string> res;
  XrmDatabase db = nullptr;
  {
    char* resource_manager = XResourceManagerString(dpy);
    if (resource_manager) {
      XrmInitialize();
      db = XrmGetStringDatabase(resource_manager);
    }
  }
  AddResource(&res, db, XRES_FONT, "Font", "roboto-16");
  AddResource(&res, db, XRES_BG, "String", "white");
  AddResource(&res, db, XRES_FG, "String", "black");
  AddResource(&res, db, XRES_SEL_BG, "String", "#b3e0ff");
  AddResource(&res, db, XRES_SEL_FG, "String", "black");
  return res;
}

static void RestartSelf(int signum) {
  forceRestart = true;
}

static void SetWindowProps(Window window, unsigned long height) {
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

  // _NET_WM_STRUT provides left, right, top, bottom. As our window only appears
  // at the top, we only set top to the height of the window.
  unsigned long val[4] = {0, 0, height, 0};
  Atom strutAtom = XInternAtom(dpy, "_NET_WM_STRUT", true);
  if (strutAtom == None) {
    LOGE() << "no strut property";
    return;
  }
  XChangeProperty(dpy, window, strutAtom, XA_CARDINAL, 32, PropModeReplace,
                  (unsigned char*)val, 4);
}

static void SetDropDownWindowProps(Window window) {
  // _NET_WM_WINDOW_TYPE describes that this is a kind of dock or panel window,
  // that should probably be kept on top, but that the window manager certainly
  // shouldn't decorate with frame, title bar etc.
  Atom typeAtom = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE", 0);
  Atom typeDockAtom = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_MENU", 0);
  XChangeProperty(dpy, window, typeAtom, XA_ATOM, 32, PropModeReplace,
                  (unsigned char*)&typeDockAtom, 1);
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

static void SetSizeFromXRandR() {
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
  display_xmin = min;
  display_width = max - min;
  XMoveResizeWindow(dpy, window, min, Y_OFFSET, display_width, window_height);
  menu->SizeChanged();
}

static void RRScreenChange(XEvent* ev) {
  XRRScreenChangeNotifyEvent* rrev = (XRRScreenChangeNotifyEvent*)ev;
  static long lastSerial;
  if (rrev->serial == lastSerial) {
    LOGI() << "Dropping duplicate event for serial " << std::hex << lastSerial;
    return;  // Drop duplicate message (we get lots of these).
  }
  lastSerial = rrev->serial;
  SetSizeFromXRandR();
}

unsigned long GetColour(const string& name) {
  XColor colour, exact;
  XAllocNamedColor(dpy, DefaultColormap(dpy, DefaultScreen(dpy)), name.c_str(),
                   &colour, &exact);
  return colour.pixel;
}

// Returns a short comprising two copies of the lowest byte in c.
// This converts an 8-bit r, g or b component into a 16-bit value as required
// by XRenderColor.
static unsigned short extend(unsigned long c) {
  unsigned short result = c & 0xff;
  return result | (result << 8);
}

XRenderColor GetXRenderColor(const string& name) {
  const unsigned long rgb = GetColour(name);
  return XRenderColor{extend(rgb >> 16), extend(rgb >> 8), extend(rgb), 0xffff};
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

  map<string, string> x_resources = GetResources();

  // Find the screen's dimensions.
  int screen = DefaultScreen(dpy);
  display_xmin = 0;
  display_width = DisplayWidth(dpy, screen);

  // Set up an error handler.
  XSetErrorHandler(ErrorHandler);

  // Get font.
  g_font = XftFontOpenName(dpy, screen, x_resources[XRES_FONT].c_str());
  if (g_font == nullptr) {
    LOGE() << "couldn't find font " << x_resources[XRES_FONT]
           << "; trying default";
    g_font = XftFontOpenName(dpy, 0, "fixed");
    LOGF_IF(g_font == nullptr) << "can't find a font";
  }

  // Copy colours to static constants so we can access them from everywhere.
  colour_bg = GetColour(x_resources[XRES_BG]);
  colour_fg = GetColour(x_resources[XRES_FG]);
  colour_sel_bg = GetColour(x_resources[XRES_SEL_BG]);
  colour_sel_fg = GetColour(x_resources[XRES_SEL_FG]);

  // Create the window.
  g_font_height = 1.2 * (g_font->ascent + g_font->descent);
  g_font_yoff = 1.2 * g_font->ascent;
  window_height = g_font_height;
  const Window root = DefaultRootWindow(dpy);
  XSetWindowAttributes attr;
  attr.override_redirect = False;
  attr.background_pixel = colour_bg;
  attr.border_pixel = BlackPixel(dpy, screen);
  attr.event_mask = ExposureMask | VisibilityChangeMask | PointerMotionMask |
                    ButtonPressMask | ButtonReleaseMask | StructureNotifyMask |
                    EnterWindowMask | LeaveWindowMask;
  window = XCreateWindow(
      dpy, root, 0, 0, display_width, window_height, 0, CopyFromParent,
      InputOutput, CopyFromParent,
      CWOverrideRedirect | CWBackPixel | CWBorderPixel | CWEventMask, &attr);

  // Set struts on the window, so the window manager knows where not to place
  // other windows.
  SetWindowProps(window, window_height);

  // Create the objects needed to render text in the window.
  XRenderColor xrc = GetXRenderColor(x_resources[XRES_FG]);
  XftColorAllocValue(dpy, DefaultVisual(dpy, screen),
                     DefaultColormap(dpy, screen), &xrc, &g_font_color);
  xrc = GetXRenderColor(x_resources[XRES_SEL_FG]);
  XftColorAllocValue(dpy, DefaultVisual(dpy, screen),
                     DefaultColormap(dpy, screen), &xrc,
                     &g_selected_font_color);

  dropdown_window = XCreateWindow(
      dpy, root, 0, 0, 100, 100, 0, CopyFromParent, InputOutput, CopyFromParent,
      CWOverrideRedirect | CWBackPixel | CWBorderPixel | CWEventMask, &attr);
  SetDropDownWindowProps(dropdown_window);
  g_dropdown_font_draw =
      XftDrawCreate(dpy, dropdown_window, DefaultVisual(dpy, screen),
                    DefaultColormap(dpy, screen));

  // Create GCs (note: the GCs for the main menu window are created in the Menu
  // class, as we use a pixmap for drawing so as to avoid flickering when the
  // clock updates.
  XGCValues gv;
  // dropdown_gc is used for clearing the drop-down window background, and for
  // drawing the partial border around the edge of the window.
  gv.foreground = colour_fg;
  gv.background = colour_bg;
  dropdown_gc =
      XCreateGC(dpy, dropdown_window, GCForeground | GCBackground, &gv);
  // dropdown_highlight_gc is used to draw a rectangle behind the text of the
  // currently-highlighted item.
  gv.foreground = colour_sel_bg;
  dropdown_highlight_gc =
      XCreateGC(dpy, dropdown_window, GCForeground | GCBackground, &gv);

  // Create the menu items.
  ReadConfig();

  // Do we need to support XRandR?
  int rr_event_base, rr_error_base;
  bool have_rr = XRRQueryExtension(dpy, &rr_event_base, &rr_error_base);
  if (have_rr) {
    XRRSelectInput(dpy, root, RRScreenChangeNotifyMask);
    SetSizeFromXRandR();
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
      RRScreenChange(&ev);
      continue;
    }
    if (updaters->Update()) {
      DoExpose(nullptr);
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
