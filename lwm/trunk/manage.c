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
#include <string.h>

#include <X11/X.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/Xatom.h>

/* These are Motif definitions from Xm/MwmUtil.h, but Motif isn't available
   everywhere. */
#define MWM_HINTS_FUNCTIONS     (1L << 0)
#define MWM_HINTS_DECORATIONS   (1L << 1)
#define MWM_HINTS_INPUT_MODE    (1L << 2)
#define MWM_HINTS_STATUS        (1L << 3)
#define MWM_DECOR_ALL           (1L << 0)
#define MWM_DECOR_BORDER        (1L << 1)
#define MWM_DECOR_RESIZEH       (1L << 2)
#define MWM_DECOR_TITLE         (1L << 3)
#define MWM_DECOR_MENU          (1L << 4)
#define MWM_DECOR_MINIMIZE      (1L << 5)
#define MWM_DECOR_MAXIMIZE      (1L << 6)

#include "lwm.h"

static int getProperty(Window, Atom, Atom, long, unsigned char **);
static int getWindowState(Window, int *);
static void applyGravity(Client *);

/*ARGSUSED*/
void
manage(Client * c, int mapped)
{
	int state;
	XWMHints * hints;
	XWindowAttributes current_attr;
	XSetWindowAttributes attr;

	/* For WM_PROTOCOLS handling. */
	Atom * protocols;
	int n;
	int p;

	/* Where auto-placement is going to put the next window. */
	static int	auto_x = 100;
	static int	auto_y = 100;

	/* get the EWMH window type, as this might overrule some hints */
	c->wtype = ewmh_get_window_type(c->window);
	/* get in the initial EWMH state */
	ewmh_get_state(c);
	/* set EWMH allowable actions, now we intend to manage this window */
	ewmh_set_allowed(c);
	/* is this window to have a frame? */
	if (c->wtype == WTypeNone) {
		/* this breaks the ewmh spec (section 5.6) because in the
		 * absence of a _NET_WM_WINDOW_TYPE, _WM_WINDOW_TYPE_NORMAL
		 * must be taken. bummer.
		 */
		c->framed = motifWouldDecorate(c);
	} else {
		c->framed = ewmh_hasframe(c);
	}
	if (isShaped(c->window)) c->framed = False;

	/* get the EWMH strut - if there is one */
	ewmh_get_strut(c);

	/*
	 * Get the hints, window name, and normal hints (see ICCCM
	 * section 4.1.2.3).
	 */
	hints = XGetWMHints(dpy, c->window);

	getWindowName(c);
	getNormalHints(c);

	/*
	 * Get the colourmaps associated with this window. Get the window
	 * attribute colourmap first, then look to see if the
	 * WM_COLORMAP_WINDOWS property has been used to specify
	 * windows needing colourmaps that differ from the top-level
	 * colourmap. (See ICCCM section 4.1.8.)
	 */
	XGetWindowAttributes(dpy, c->window, &current_attr);
	c->cmap = current_attr.colormap;

	getColourmaps(c);

	/*
	 * Scan the list of atoms on WM_PROTOCOLS to see which of the
	 * protocols that we understand the client is prepared to
	 * participate in. (See ICCCM section 4.1.2.7.)
	 */
	if (XGetWMProtocols(dpy, c->window, &protocols, &n) != 0) {
		for (p = 0; p < n; p++) {
			if (protocols[p] == wm_delete) {
				c->proto |= Pdelete;
			} else if (protocols[p] == wm_take_focus) {
				c->proto |= Ptakefocus;
			}
		}

		XFree(protocols);
	}

	/* Get the WM_TRANSIENT_FOR property (see ICCCM section 4.1.2.6). */
	getTransientFor(c);

	/* Work out details for the Client structure from the hints. */
	if (hints && (hints->flags & InputHint))
		c->accepts_focus = hints->input;
	if (c->proto | Ptakefocus)
		/* WM_TAKE_FOCUS overrides normal hints */
		c->accepts_focus = True;

	if (!getWindowState(c->window, &state))
		state = hints ? hints->initial_state : NormalState;

	/*
	 *	Sort out the window's position.
	 */
	{
		Window root_window;
		int x, y;
		unsigned int w, h;
		unsigned int border_width, depth;

		XGetGeometry(dpy, c->window, &root_window, &x, &y, &w, &h,
			&border_width, &depth);

		/*
		 * Do the size first.
		 *
		 * "The size specifiers refer to the width and height of the
		 * client excluding borders" -- ICCCM 4.1.2.3.
		 */
		c->size.width  = w;
		c->size.height = h;
		if (c->framed == True) {
			c->size.width  += 2 * border;
			c->size.height += 2 * border;
		}

		/*
		 * THIS IS A HACK!
		 *
		 * OpenGL programs have a habit of appearing smaller than their
		 * minimum sizes, which they don't like.
		 */
		if (c->size.width < c->size.min_width)
			c->size.width = c->size.min_width;
		if (c->size.height < c->size.min_height)
			c->size.height = c->size.min_height;

		/* Do the position next. */

		/*
		 * If we have a user-specified position for a top-level window,
		 * or a program-specified position for a dialogue box, we'll
		 * take it. We'll also just take it during initialisation,
		 * since the previous manage probably placed its windows
		 * sensibly.
		 */
		if (c->trans != None && c->size.flags & PPosition) {
			/* It's a "dialogue box". Trust it. */
			c->size.x = x;
			c->size.y = y;
		} else if ((c->size.flags & USPosition) ||
			c->framed == False || mode == wm_initialising ) {
			/* Use the specified window position. */
			c->size.x = x;
			c->size.y = y;

			/*
			 * We need to be careful of the right-hand edge and
			 * bottom. We can use the window gravity (if specified)
			 * to handle this. (See section 4.1.2.3 of the ICCCM.)
			 */
			applyGravity(c);
		} else {
			/* No position was specified: use the auto-placement
			 * heuristics. */

			/* firstly, make sure auto_x and auto_y are outside
			 * strut */
			if (auto_x < c->screen->strut.left)
				auto_x = c->screen->strut.left;
			if (auto_y < c->screen->strut.top)
				auto_y = c->screen->strut.top;

			if ((auto_x + c->size.width) > 
				(c->screen->display_width -
				c->screen->strut.right) && 
				(c->size.width <=
				(c->screen->display_width -
				c->screen->strut.left -
				c->screen->strut.right))) {
				/*
				 * If the window wouldn't fit using normal
				 * auto-placement but is small enough to fit
				 * horizontally, then centre the window
				 * horizontally.
				 */
				c->size.x = (c->screen->display_width
					- c->size.width) / 2;
				auto_x = c->screen->strut.left + 20;
			} else {
				c->size.x = auto_x;
				auto_x += 10;
				if (auto_x > (c->screen->display_width / 2))
					auto_x = c->screen->strut.left + 20;
			}

			if (((auto_y + c->size.height) >
				(c->screen->display_height -
				c->screen->strut.bottom)) &&
				(c->size.height <=
				(c->screen->display_height -
				c->screen->strut.top -
				c->screen->strut.bottom))) {
				/*
				 * If the window wouldn't fit using normal
				 * auto-placement but is small enough to fit
				 * vertically, then centre the window
				 * vertically.
				 */
				 c->size.y = (c->screen->display_height
					- c->size.height) / 2;
				 auto_y = c->screen->strut.top + 20;
			} else {
				c->size.y = auto_y;
				auto_y += 10;
				if (auto_y > (c->screen->display_height / 2))
					auto_y = c->screen->strut.top + 20;
			}
		}
	}

	if (hints)
		XFree(hints);

	/*
	 * Do all the reparenting and stuff.
	 */

	if (c->framed == True) {
		c->parent = XCreateSimpleWindow(dpy, c->screen->root,
			c->size.x, c->size.y - titleHeight(),
			c->size.width, c->size.height + titleHeight(),
			1, c->screen->black, c->screen->white);

		attr.event_mask = ExposureMask | EnterWindowMask | ButtonMask |
			SubstructureRedirectMask | SubstructureNotifyMask |
			PointerMotionMask;
		XChangeWindowAttributes(dpy, c->parent, CWEventMask, &attr);

		XResizeWindow(dpy, c->window, c->size.width - 2 * border,
			c->size.height - 2 * border);
	}

	/*
	 * Stupid X11 doesn't let us change border width in the above
	 * call. It's a window attribute, but it's somehow second-class.
	 *
	 * As pointed out by Adrian Colley, we can't change the window
	 * border width at all for InputOnly windows.
	 */
	if (current_attr.class != InputOnly)
		XSetWindowBorderWidth(dpy, c->window, 0);

	attr.event_mask = ColormapChangeMask | EnterWindowMask |
		PropertyChangeMask | FocusChangeMask;
	attr.win_gravity = StaticGravity;
	attr.do_not_propagate_mask = ButtonMask;
	XChangeWindowAttributes(dpy, c->window,
		CWEventMask | CWWinGravity | CWDontPropagate, &attr);

	if (c->framed == True) {
		XReparentWindow(dpy, c->window, c->parent,
			border, border + titleHeight());
	} else {
		XReparentWindow(dpy, c->window, c->parent,
			c->size.x, c->size.y);
	}

	setShape(c);

	XAddToSaveSet(dpy, c->window);
	if (state == IconicState) {
		hide(c);
	} else {
		/* Map the new window in the relevant state. */
		c->hidden = False;
		XMapWindow(dpy, c->parent);
		XMapWindow(dpy, c->window);
		setactive(c, (focus_mode == focus_click) ? 1 : 0, 0L);
		Client_SetState(c, NormalState);
	}

	if (c->wstate.fullscreen == True) Client_EnterFullScreen(c);

	if (current != c)
		cmapfocus(current);
}

static void
applyGravity(Client *c) {
	if (c->size.flags & PWinGravity) {
		switch (c->size.win_gravity) {
			case NorthEastGravity:
				c->size.x -= 2 * border;
				break;
			case SouthWestGravity:
				c->size.y -= 2 * border;
				break;
			case SouthEastGravity:
				c->size.x -= 2 * border;
				c->size.y -= 2 * border;
				break;
		}
	}
}

void
getTransientFor(Client *c) {
	Window	trans = None;

	XGetTransientForHint(dpy, c->window, &trans);
	c->trans = trans;
}

void
withdraw(Client *c) {
	if (c->parent != c->screen->root) {
		XUnmapWindow(dpy, c->parent);
		XReparentWindow(dpy, c->parent, c->screen->root, c->size.x, c->size.y);
	}
	
	XRemoveFromSaveSet(dpy, c->window);
	Client_SetState(c, WithdrawnState);
	
	/*
	 * Flush and ignore any errors. X11 sends us an UnmapNotify before it
	 * sends us a DestroyNotify. That means we can get here without knowing
	 * whether the relevant window still exists.
	 */
	ignore_badwindow = 1;
	XSync(dpy, False);
	ignore_badwindow = 0;
}

static void
installColourmap(Colormap cmap) {
	if (cmap == None)
		cmap = DefaultColormap(dpy, DefaultScreen(dpy));
	XInstallColormap(dpy, cmap);
}

void
cmapfocus(Client * c) {
	int	i;
	int	found;
	Client	*cc;

	if (c == 0)
		installColourmap(None);
	else if (c->ncmapwins != 0) {
		found = 0;
		for (i = c->ncmapwins - 1; i >= 0; i--) {
			installColourmap(c->wmcmaps[i]);
			if (c->cmapwins[i] == c->window)
				found++;
		}
		if (!found)
			installColourmap(c->cmap);
	} else if (c->trans != None && (cc = Client_Get(c->trans)) != 0 &&
	    cc->ncmapwins != 0)
		cmapfocus(cc);
	else
		installColourmap(c->cmap);
}

void
getColourmaps(Client *c) {
	int	n;
	int	i;
	Window	*cw;
	XWindowAttributes	attr;

	if (c == 0)
		return;

	n = getProperty(c->window, wm_colormaps, XA_WINDOW, 100L, (unsigned char **) &cw);
	if (c->ncmapwins != 0) {
		XFree(c->cmapwins);
		free(c->wmcmaps);
	}
	if (n <= 0) {
		c->ncmapwins = 0;
		return;
	}
	c->ncmapwins = n;
	c->cmapwins = cw;

	c->wmcmaps = (Colormap *) malloc(n * sizeof(Colormap));
	for (i = 0; i < n; i++) {
		if (cw[i] == c->window) {
			c->wmcmaps[i] = c->cmap;
		} else {
			XSelectInput(dpy, cw[i], ColormapChangeMask);
			XGetWindowAttributes(dpy, cw[i], &attr);
			c->wmcmaps[i] = attr.colormap;
		}
	}
}

/*ARGSUSED*/
void
Terminate(int signal) {
	/* Set all clients free. */
	Client_FreeAll();
	
	/* Give up the input focus and the colourmap. */
	XSetInputFocus(dpy, PointerRoot, RevertToPointerRoot, CurrentTime);
	installColourmap(None);
	
	XCloseDisplay(dpy);

	session_end();

	if (signal) {
		exit(EXIT_FAILURE);
	} else {
		exit(EXIT_SUCCESS);
	}
}


static int
getProperty(Window w, Atom a, Atom type, long len, unsigned char **p) {
	Atom	real_type;
	int	format;
	unsigned long	n;
	unsigned long	extra;
	int	status;

	/*
	 *	len is in 32-bit multiples.
	 */
	status = XGetWindowProperty(dpy, w, a, 0L, len, False, type, &real_type, &format, &n, &extra, p);
	if (status != Success || *p == 0)
		return -1;
	if (n == 0)
		XFree(*p);
	/*
	 *	could check real_type, format, extra here...
	 */
	return n;
}

void
getWindowName(Client *c) {
	char * name;
	Atom actual_type;
	int format;
	unsigned long n;
	unsigned long extra;
	int was_nameless;
	
	if (c == 0)
		return;
	
	was_nameless = (c->name == 0);
	
	if (ewmh_get_window_name(c) == False &&
		XGetWindowProperty(dpy, c->window, _mozilla_url, 0L, 100L, False, AnyPropertyType, &actual_type, &format, &n, &extra, (unsigned char **) &name) == Success && name && *name != '\0' && n != 0) {
		Client_Name(c, name, False);
		XFree(name);
	} else if (XGetWindowProperty(dpy, c->window, XA_WM_NAME, 0L, 100L, False, AnyPropertyType, &actual_type, &format, &n, &extra, (unsigned char **) &name) == Success && name && *name != '\0' && n != 0) {
		/* That rather unpleasant condition is necessary because xwsh uses
	 	* COMPOUND_TEXT rather than STRING for its WM_NAME property,
	 	* and anonymous xwsh windows are annoying.
	 	*/
		if (actual_type == compound_text && memcmp(name, "\x1b\x28\x42", 3) == 0) {
			Client_Name(c, name + 3, False);
		} else {
			Client_Name(c, name, False);
		}
		XFree(name);
	}
	
	if (!was_nameless)
		Client_DrawBorder(c, c == current);
}

void
getNormalHints(Client *c) {
	int x, y, w, h;
	long msize;

	/* We have to be a little careful here. The ICCCM says that the x, y
	 * and width, height components aren't used. So we use them. That means
	 * that we need to save and restore them whenever we fill the size
	 * struct. */
	x = c->size.x;
	y = c->size.y;
	w = c->size.width;
	h = c->size.height;

	/* Do the get. */
	if (XGetWMNormalHints(dpy, c->window, &c->size, &msize) == 0)
		c->size.flags = 0;

	if (c->framed == True) {
		/*
	 	* Correct the minimum allowable size of this client to take
		* account of the window border.
	 	*/
		if (c->size.flags & PMinSize) {
			c->size.min_width  += 2 * border;
			c->size.min_height += 2 * border;
		} else {
			c->size.flags |= PMinSize;
			c->size.min_width  = 2 * (2 * border);
			if (c->accepts_focus)
				c->size.min_height = 2 * (2*border);
			else
				c->size.min_height = 2 * (2*border);
		}

		/*
	 	* Correct the maximum allowable size of this client to take
		* account of the window border.
	 	*/
		if (c->size.flags & PMaxSize) {
			c->size.max_width  += 2 * border;
			c->size.max_height += 2 * border;
		}
	}

	/*
	 * Ensure that the base width & height and the width & height increments
	 * are set correctly so that we don't have to do this in MakeSane.
	 */
	if (!(c->size.flags & PBaseSize))
		c->size.base_width = c->size.base_height = 0;

	if (!(c->size.flags & PResizeInc))
		c->size.width_inc = c->size.height_inc = 1;

	/*
	 * If the client gives identical minimum and maximum sizes, we don't
	 * want the user to resize in that direction.
	 */
	if (c->size.min_width == c->size.max_width)
		c->size.width_inc = 0;

	if (c->size.min_height == c->size.max_height)
		c->size.height_inc = 0;

	/* Restore the window-manager bits. */
	c->size.x = x;
	c->size.y = y;
	c->size.width = w;
	c->size.height = h;
}

static int
getWindowState(Window w, int *state) {
	long	*p = 0;

	if (getProperty(w, wm_state, wm_state, 2L, (unsigned char **) &p) <= 0)
		return 0;

	*state = (int) *p;
	XFree(p);
	return 1;
}


extern Bool
motifWouldDecorate(Client *c) {
	unsigned long *p = 0;
	Bool ret = True; /* if all else fails - decorate */

	if (getProperty(c->window, motif_wm_hints, motif_wm_hints,
		5L, (unsigned char **) &p) <= 0) 
		return ret;

	if ((p[0] & MWM_HINTS_DECORATIONS) &&
		!(p[2] & (MWM_DECOR_BORDER | MWM_DECOR_ALL)))
		ret = False;

	XFree(p);
	return ret;
}
