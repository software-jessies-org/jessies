/*
 * lwm, a window manager for X11
 * Copyright (C) 1997-2003 Elliott Hughes, James Carter
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

#include <stdio.h>
#include <stdlib.h>
#include <locale.h>
#include <errno.h>

#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>

#include <signal.h>

#include <X11/X.h>
#include <X11/Xos.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/Xatom.h>

#include "lwm.h"

Mode mode;			/* The window manager's mode. (See "lwm.h".) */
int start_x;			/* The X position where the mode changed. */
int start_y;			/* The Y position where the mode changed. */

Display * dpy;			/* The connection to the X server. */
int screen_count;		/* The number of screens. */
ScreenInfo * screens;		/* Information about these screens. */
ScreenInfo * current_screen;

XFontSet font_set = NULL;	/* Font set for title var */
XFontSetExtents *font_set_ext = NULL;
XFontSet popup_font_set = NULL;	/* Font set for popups */
XFontSetExtents *popup_font_set_ext = NULL;

Bool shape;			/* Does server have Shape Window extension? */
int shape_event;		/* ShapeEvent event type. */

/* Atoms we're interested in. See the ICCCM for more information. */
Atom wm_state;
Atom wm_change_state;
Atom wm_protocols;
Atom wm_delete;
Atom wm_take_focus;
Atom wm_colormaps;
Atom compound_text;

/** Netscape uses this to give information about the URL it's displaying. */
Atom _mozilla_url;

/*
 * if we're really short of a clue we might look at motif hints, and
 * we're not going to link with motif, so we'll have to do it by hand
 */
Atom motif_wm_hints;

char *argv0;

static void initScreens(void);
static void initScreen(int);

/*ARGSUSED*/
extern int
main(int argc, char *argv[]) {
	XEvent ev;
	struct sigaction sa;

	argv0 = argv[0];

	mode = wm_initialising;

	setlocale(LC_ALL,"");

	/* Open a connection to the X server. */
	dpy = XOpenDisplay(NULL);
	if (dpy == 0)
		panic("can't open display.");

	parseResources();

	/* Set up an error handler. */
	XSetErrorHandler(errorHandler);

	/* Set up signal handlers. */
	signal(SIGTERM, Terminate);
	signal(SIGINT, Terminate);
	signal(SIGHUP, Terminate);

	/* Ignore SIGCHLD. */
	sa.sa_handler = SIG_IGN;
#ifdef SA_NOCLDWAIT
	sa.sa_flags = SA_NOCLDWAIT;
#else
	sa.sa_flags = 0;
#endif
	sigemptyset(&sa.sa_mask);
	sigaction(SIGCHLD, &sa, 0);

	/* Internalize useful atoms. */
	wm_state = XInternAtom(dpy, "WM_STATE", False);
	wm_change_state = XInternAtom(dpy, "WM_CHANGE_STATE", False);
	wm_protocols = XInternAtom(dpy, "WM_PROTOCOLS", False);
	wm_delete = XInternAtom(dpy, "WM_DELETE_WINDOW", False);
	wm_take_focus = XInternAtom(dpy, "WM_TAKE_FOCUS", False);
	wm_colormaps = XInternAtom(dpy, "WM_COLORMAP_WINDOWS", False);
	compound_text = XInternAtom(dpy, "COMPOUND_TEXT", False);
	
	_mozilla_url = XInternAtom(dpy, "_MOZILLA_URL", False);

	motif_wm_hints = XInternAtom(dpy, "_MOTIF_WM_HINTS", False);

	ewmh_init();
	
	/*
	 * Get fonts for our titlebar and our popup window. We try to
	 * get Lucida, but if we can't we make do with fixed because everyone
	 * has that.
	 */
	{
		/* FIXME: do these need to be freed? */
		char **missing;
		char *def;
		int missing_count;

		font_set = XCreateFontSet(dpy, font_name,
			&missing, &missing_count, &def);
		if (font_set == NULL)
			font_set = XCreateFontSet(dpy, "fixed",
				&missing, &missing_count, &def);
		if (font_set == NULL)
			panic("unable to create font set for title font");
		if (missing_count > 0)
			fprintf(stderr,"%s: warning: missing %d charset"
				"%s for title font\n", argv0, missing_count,
				(missing_count == 1)?"":"s");
		font_set_ext = XExtentsOfFontSet(font_set);

		popup_font_set = XCreateFontSet(dpy, popup_font_name,
			&missing, &missing_count, &def);
		if (popup_font_set == NULL)
			popup_font_set = XCreateFontSet(dpy, "fixed",
				&missing, &missing_count, &def);
		if (popup_font_set == NULL)
			panic("unable to create font set for popup font");
		if (missing_count > 0)
			fprintf(stderr,"%s: warning: missing %d charset"
				"%s for popup font\n", argv0, missing_count,
				(missing_count == 1)?"":"s");
		popup_font_set_ext = XExtentsOfFontSet(popup_font_set);
	}
	
	initScreens();
	ewmh_init_screens();
	session_init(argc, argv);
	
	/* See if the server has the Shape Window extension. */
	shape = serverSupportsShapes();
	
	/*
	 * Initialisation is finished, but we start off not interacting with the
	 * user.
	 */
	mode = wm_idle;
	
	/*
	 * The main event loop.
	 */
	for (;;) {
		fd_set readfds;
		struct timeval timeout;

		FD_ZERO(&readfds);
		if (ice_fd > 0) FD_SET(ice_fd, &readfds);
		timeout.tv_sec = 0;
		timeout.tv_usec = 10;
		switch (select(ice_fd + 1, &readfds, NULL, NULL, &timeout)) {
		case 0:
			while (XPending(dpy) > 0) {
				XNextEvent(dpy, &ev);
				dispatch(&ev);
			}
			break;
		case 1:
			session_process();
			break;
		}
	}
}

void
sendConfigureNotify(Client *c) {
	XConfigureEvent ce;

	ce.type = ConfigureNotify;
	ce.event = c->window;
	ce.window = c->window;
	if (c->framed == True) {
		ce.x = c->size.x + border;
		ce.y = c->size.y + border;
		ce.width = c->size.width - 2 * border;
		ce.height = c->size.height - 2 * border;
		ce.border_width = c->border;
	} else {
		ce.x = c->size.x;
		ce.y = c->size.y;
		ce.width = c->size.width;
		ce.height = c->size.height;
		ce.border_width = c->border;
	}
	ce.above = None;
	ce.override_redirect = 0;
	XSendEvent(dpy, c->window, False, StructureNotifyMask, (XEvent *) &ce);
}

extern void
scanWindowTree(int screen) {
	unsigned int i;
	unsigned int nwins;
	Client * c;
	Window dw1;
	Window dw2;
	Window * wins;
	XWindowAttributes attr;
	
	XQueryTree(dpy, screens[screen].root, &dw1, &dw2, &wins, &nwins);
	for (i = 0; i < nwins; i++) {
		XGetWindowAttributes(dpy, wins[i], &attr);
		if (attr.override_redirect /*|| isShaped(wins[i])*/ || wins[i] == screens[screen].popup)
			continue;
		c = Client_Add(wins[i], screens[screen].root);
		if (c != 0 && c->window == wins[i]) {
			c->screen = &screens[screen];
			c->size.x = attr.x;
			c->size.y = attr.y;
/* we'll leave it until it's managed
			if (c->framed == True) {
				c->size.x -= border;
				c->size.y -= border;
			}
*/
			c->size.width  = attr.width;
			c->size.height = attr.height;
/* we'll leave it until it's managed
			if (c->framed == True) {
				c->size.width  += 2 * border;
				c->size.height += 2 * border;
			}
*/
			c->border = attr.border_width;
			if (attr.map_state == IsViewable) {
				c->internal_state = IPendingReparenting;
				manage(c, 1);
			}
		}
	}
	XFree(wins);
}

/*ARGSUSED*/
extern void
shell(ScreenInfo * screen, int button, int x, int y) {
	char * command = NULL;
	char * sh;
	
	/* Get the command we're to execute. Give up if there isn't one. */
	if (button == Button1)
		command = btn1_command;
	if (button == Button2)
		command = btn2_command;
	if (command == NULL)
		return;
	
	sh = getenv("SHELL");
	if (sh == 0)
		sh = "/bin/sh";
	
	switch (fork()) {
	case 0:		/* Child. */
		close(ConnectionNumber(dpy));
		if (screen && screen->display_spec != 0)
			putenv(screen->display_spec);
		execl(sh, sh, "-c", command, 0);
		fprintf(stderr, "%s: can't exec \"%s -c %s\"\n", argv0, sh,
			command);
		execlp("xterm", "xterm", 0);
		exit(EXIT_FAILURE);
	case -1:	/* Error. */
		fprintf(stderr, "%s: couldn't fork\n", argv0);
		break;
	}
}

extern int
titleHeight(void) {
	return font_set_ext->max_logical_extent.height;
}

extern int
ascent(XFontSetExtents *font_set_ext) {
	return abs(font_set_ext->max_logical_extent.y);
}

extern int
popupHeight(void) {
	return popup_font_set_ext->max_logical_extent.height;
}

extern int
titleWidth(XFontSet font_set, Client *c) {
	XRectangle ink;
	XRectangle logical;
	char *name;
	int namelen;

	if (c == NULL) return 0;
	if (c->menu_name == NULL) {
		name = c->name;
		namelen = c->namelen;
	} else {
		name = c->menu_name;
		namelen = c->menu_namelen;
	}
	if (name == NULL) return 0;
#ifdef X_HAVE_UTF8_STRING
	if (c->name_utf8 == True) 
		Xutf8TextExtents(font_set, name, namelen,
			&ink, &logical);
	else
#endif
		XmbTextExtents(font_set, name, namelen,
			&ink, &logical);

	return logical.width;
}

extern int
popupWidth(char *string, int string_length) {
	XRectangle ink;
	XRectangle logical;

	XmbTextExtents(popup_font_set, string, string_length,
		&ink, &logical);

	return logical.width;
}

static void
initScreens(void) {
	int screen;
	
	/* Find out how many screens we've got, and allocate space for their info. */
	screen_count = ScreenCount(dpy);
	screens = (ScreenInfo *) malloc(screen_count * sizeof(ScreenInfo));
	
	/* Go through the screens one-by-one, initialising them. */
	for (screen = 0; screen < screen_count; screen++) {
		initialiseCursors(screen);
		initScreen(screen);
		scanWindowTree(screen);
	}
}

static void
initScreen(int screen) {
	XGCValues gv;
	XSetWindowAttributes attr;
	XColor colour, exact;
	int len;
	char * display_string = DisplayString(dpy);
	char * colon = strrchr(display_string, ':');
	char * dot = strrchr(display_string, '.');
	
	/* Set the DISPLAY specification. */
	if (colon) {
		len = 9 + strlen(display_string) + ((dot == 0) ? 2 : 0) + 10;
		screens[screen].display_spec = (char *) malloc(len);
		sprintf(screens[screen].display_spec, "DISPLAY=%s", display_string);
		if (dot == 0) dot = screens[screen].display_spec + len - 3;
		else dot = strrchr(screens[screen].display_spec, '.');
		sprintf(dot, ".%i", screen);
	} else {
		screens[screen].display_spec = 0;
	}

	/* Find the root window. */
	screens[screen].root = RootWindow(dpy, screen);
	screens[screen].display_width = DisplayWidth(dpy, screen);
	screens[screen].display_height = DisplayHeight(dpy, screen);
	screens[screen].strut.left = 0;
	screens[screen].strut.right = 0;
	screens[screen].strut.top = 0;
	screens[screen].strut.bottom = 0;
	
	/* Get the pixel values of the only two colours we use. */
	screens[screen].black = BlackPixel(dpy, screen);
	screens[screen].white = WhitePixel(dpy, screen);
	XAllocNamedColor(dpy, DefaultColormap(dpy, screen), "DimGray", &colour, &exact);
	screens[screen].gray = colour.pixel;
	
	/* Set up root (frame) GC's. */
	gv.foreground = screens[screen].black ^ screens[screen].white;
	gv.background = screens[screen].white;
	gv.function = GXxor;
	gv.line_width = 1;
	gv.subwindow_mode = IncludeInferiors;
	screens[screen].gc_thin = XCreateGC(dpy, screens[screen].root,
		GCForeground | GCBackground | GCFunction |
		GCLineWidth | GCSubwindowMode, &gv);
	
	gv.line_width = 2;
	screens[screen].gc = XCreateGC(dpy, screens[screen].root,
		GCForeground | GCBackground | GCFunction |
		GCLineWidth | GCSubwindowMode, &gv);
	
	/* Create a window for our popup. */
	screens[screen].popup = XCreateSimpleWindow(dpy, screens[screen].root,
		0, 0, 1, 1, 1, screens[screen].black, screens[screen].white);
	attr.event_mask = ButtonMask | ButtonMotionMask | ExposureMask;
	XChangeWindowAttributes(dpy, screens[screen].popup, CWEventMask, &attr);
	
	/* Create menu GC. */
	gv.line_width = 1;
	screens[screen].menu_gc = XCreateGC(dpy, screens[screen].popup,
		GCForeground | GCBackground | GCFunction |
		GCLineWidth | GCSubwindowMode, &gv);
	
	/* Create size indicator GC. */
	gv.foreground = screens[screen].black;
	gv.function = GXcopy;
	screens[screen].size_gc = XCreateGC(dpy, screens[screen].popup,
		GCForeground | GCBackground | GCFunction |
		GCLineWidth | GCSubwindowMode, &gv);
	
	/* Announce our interest in the root window. */
	attr.cursor = screens[screen].root_cursor;
	attr.event_mask = SubstructureRedirectMask | SubstructureNotifyMask |
		ColormapChangeMask | ButtonPressMask | PropertyChangeMask |
		EnterWindowMask;
	XChangeWindowAttributes(dpy, screens[screen].root, CWCursor |
		CWEventMask, &attr);
	
	/* Make sure all our communication to the server got through. */
	XSync(dpy, False);
}

/**
Find the screen for which root is the root window.
*/
ScreenInfo *
getScreenFromRoot(Window root) {
	int screen;
	
	for (screen = 0; screen < screen_count; screen++)
		if (screens[screen].root == root)
			return &screens[screen];
	
	return 0;
}
