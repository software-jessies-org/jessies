#include <deque>
#include <iostream>
#include <sstream>
#include <string>

#include <stdlib.h>
#include <string.h>
#include <unistd.h>

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

static void start_startup(const std::string& icon_filename, const std::string& name) {
    std::string startup_id = invent_startup_id();
    XDisplay xdisplay;
    std::ostringstream oss;
    oss << "new:";
    oss << " ID=" << escape_for_xmessage(startup_id);
    oss << " SCREEN=" << DefaultScreenOfDisplay(xdisplay.display);
    oss << " NAME=" << escape_for_xmessage(name);
    oss << " ICON=" << icon_filename;
    broadcast_xmessage(oss.str());
    std::cout << startup_id << std::endl;
}

static void finish_startup(const std::string& startup_id) {
    broadcast_xmessage("remove: ID=" + escape_for_xmessage(startup_id));
}

static void show_usage_and_exit() {
    std::cerr << "usage: gnome-startup [start <icon-filename> <text...>|stop <id>...]" << std::endl;
    exit(EXIT_FAILURE);
}

int main(int argc, char* argv[]) {
    typedef std::deque<std::string> ArgList;
    ArgList args(&argv[1], argv + argc);
    if (args.size() >= 3 && args.front() == "start") {
        args.pop_front(); // "start"
        std::string icon_filename = args.front(); args.pop_front();
        std::string name = join(" ", args);
        start_startup(icon_filename, name);
    } else if (args.size() >= 2 && args.front() == "stop") {
        args.pop_front(); // "stop"
        for (ArgList::iterator it = args.begin(); it != args.end(); ++it) {
            finish_startup(*it);
        }
    } else {
        show_usage_and_exit();
    }
    exit(EXIT_SUCCESS);
}
