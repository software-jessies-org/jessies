#include <deque>
#include <iostream>
#include <sstream>
#include <string>
#include <X11/Xlib.h>

#include "join.h"

// Finishes the GNOME startup sessions whose ids are given on the command line.
// Based on the function gdk_notify_startup_complete from:
// http://cvs.gnome.org/viewcvs/gtk%2B/gdk/x11/gdkdisplay-x11.c?view=markup

class XDisplay {
public:
    XDisplay() {
        display = XOpenDisplay(0);
    }

    ~XDisplay() {
        XFlush(display);
        XCloseDisplay(display);
    }

    Atom getAtomByName(const char* atom_name) {
        return XInternAtom(display, atom_name, False);
    }

    Display* display;
};

static std::string escape_for_xmessage(const std::string& s) {
    std::ostringstream oss;
    for (std::string::const_iterator it = s.begin(); it != s.end(); ++it) {
        if (*it == ' ' || *it == '"' || *it == '\\') {
            oss << '\\';
        }
        oss << *it;
    }
    return oss.str();
}

static void broadcast_xmessage(const std::string& message) {
    XDisplay xdisplay;
    Window xroot_window = DefaultRootWindow(xdisplay.display);

    XSetWindowAttributes attrs;
    attrs.override_redirect = True;
    attrs.event_mask = PropertyChangeMask | StructureNotifyMask;
    Window xwindow = XCreateWindow(xdisplay.display, xroot_window, -100, -100, 1, 1, 0, CopyFromParent, CopyFromParent, CopyFromParent, CWOverrideRedirect | CWEventMask, &attrs);

    Atom type_atom = xdisplay.getAtomByName("_NET_STARTUP_INFO");
    Atom type_atom_begin = xdisplay.getAtomByName("_NET_STARTUP_INFO_BEGIN");

    XEvent xevent;
    xevent.xclient.type = ClientMessage;
    xevent.xclient.message_type = type_atom_begin;
    xevent.xclient.display = xdisplay.display;
    xevent.xclient.window = xwindow;
    xevent.xclient.format = 8;

    const char* src = message.c_str();
    const char* src_end = src + message.length() + 1; // Include trailing NUL.

    while (src != src_end) {
        char* dest = &xevent.xclient.data.b[0];
        char* dest_end = dest + 20;        
        while (dest != dest_end && src != src_end) {
            *dest++ = *src++;
        }
        while (dest != dest_end) {
            *dest++ = 0;
        }
        XSendEvent(xdisplay.display, xroot_window, False, PropertyChangeMask, &xevent);
        xevent.xclient.message_type = type_atom;
    }

    XDestroyWindow(xdisplay.display, xwindow);
}

static std::string invent_startup_id() {
    char hostname[256];
    if (gethostname(hostname, sizeof(hostname)) != 0) {
        strcpy(hostname, "localhost");
    }
    
    std::ostringstream oss;
    oss << hostname << getpid() << "_TIME" << time(0);
    return oss.str();
}

static void start_startup(const std::string& name) {
    std::string startup_id = invent_startup_id();
    XDisplay xdisplay;
    std::ostringstream oss;
    oss << "new: ID=" << escape_for_xmessage(startup_id) << " SCREEN=" << DefaultScreenOfDisplay(xdisplay.display) << " NAME=" << escape_for_xmessage(name);
    broadcast_xmessage(oss.str());
    std::cout << startup_id << std::endl;
}

static void finish_startup(const std::string& startup_id) {
    broadcast_xmessage("remove: ID=" + escape_for_xmessage(startup_id));
}

int main(int, char* args[]) {
    ++args;
    if (*args != 0 && std::string("start") == *args) {
        std::deque<std::string> words;
        while (*++args != 0) {
            words.push_back(*args);
        }
        std::string name = join(" ", words);
        start_startup(name);
    } else if (*args != 0 && std::string("stop") == *args) {
        while (*++args != 0) {
            finish_startup(*args);
        }
    } else  {
        std::cerr << "usage: gnome-startup [start <name>|stop]" << std::endl;
        exit(EXIT_FAILURE);
    }
    exit(EXIT_SUCCESS);
}
