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
#include <time.h>

#include <unistd.h>

#include <X11/X.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/Xatom.h>

#include "lwm.h"
#include "ewmh.h"

Client *current;
Client *last_focus = NULL;
static Client *clients;

static int popup_width;	/* The width of the size-feedback window. */

Edge interacting_edge;

static void sendClientMessage(Window, Atom, long, long);

Client *
client_head(void) {
	return clients;
}

void
setactive(Client *c, int on, long timestamp) {
	int inhibit;

	if (c == 0 || hidden(c))
		return;

	inhibit = !c->framed;

	if (!inhibit) {
		XMoveResizeWindow(dpy, c->parent,
			c->size.x, c->size.y - titleHeight(),
			c->size.width, c->size.height + titleHeight());
		XMoveWindow(dpy, c->window, border, border + titleHeight());
		sendConfigureNotify(c);
	}

	if (on && c->accepts_focus) {
		XSetInputFocus(dpy, c->window, RevertToPointerRoot, CurrentTime);
		if (c->proto & Ptakefocus)
			sendClientMessage(c->window, wm_protocols,
				wm_take_focus, timestamp);
		if (focus_mode == focus_click) {
			XUngrabButton(dpy, AnyButton, AnyModifier, c->window);
		}
		cmapfocus(c);
	}

	/* FIXME: is this sensible? */
	if (on && !c->accepts_focus) {
		XSetInputFocus(dpy, None, RevertToPointerRoot, CurrentTime);
	}

	if (!on && focus_mode == focus_click)
		XGrabButton(dpy, AnyButton, AnyModifier, c->window, False,
			ButtonPressMask | ButtonReleaseMask, GrabModeAsync,
			GrabModeSync, None, None);

	if (!inhibit)
		Client_DrawBorder(c, on);
}


void
Client_DrawBorder(Client *c, int active) {
	int quarter = (border + titleHeight()) / 4;

	if (c->parent == c->screen->root || c->parent == 0 ||
		c->framed == False || c->wstate.fullscreen == True)
		return;

	XSetWindowBackground(dpy, c->parent,
		active ? c->screen->black : c->screen->gray);
	XClearWindow(dpy, c->parent);

	/* Draw the ``box''. */
	if (active || focus_mode == focus_click) {
		XDrawRectangle(dpy, c->parent, c->screen->gc,
			quarter + 2, quarter, 2 * quarter, 2 * quarter);
	}

	/* Draw window title. */
	if (c->name != 0) {
#ifdef X_HAVE_UTF8_STRING
		if (c->name_utf8 == True)
			Xutf8DrawString(dpy, c->parent, font_set,
				c->screen->gc, border + 2 + (3 * quarter),
				2 + ascent(font_set_ext),
				c->name, c->namelen);
		else
#endif
			XmbDrawString(dpy, c->parent, font_set,
				c->screen->gc, border + 2 + (3 * quarter),
				2 + ascent(font_set_ext),
				c->name, c->namelen);
	}
}


Client *
Client_Get(Window w) {
	Client * c;
	
	if (w == 0 || (getScreenFromRoot(w) != 0))
		return 0;
	
	/* Search for the client corresponding to this window. */
	for (c = clients; c; c = c->next)
		if (c->window == w || c->parent == w)
			return c;
	
	/* Not found. */
	return 0;
}


Client *
Client_Add(Window w, Window root) {
	Client * c;

	if (w == 0 || w == root)
		return 0;

	/* Search for the client corresponding to this window. */
	for (c = clients; c != 0; c = c->next)
		if (c->window == w || c->parent == w)
			return c;

	c = calloc(1, sizeof *c);
	c->window = w;
	c->parent = root;
	c->framed = False;
	c->hidden = False;
	c->state = WithdrawnState;
	c->internal_state = INormal;
	c->cmap = None;
	c->name =  0;
	c->menu_name =  0;
	c->cursor = ENone;
	c->wtype = WTypeNone;
	c->wstate.skip_taskbar = False;
	c->wstate.skip_pager = False;
	c->wstate.fullscreen = False;
	c->wstate.above = False;
	c->wstate.below = False;
	c->strut.left = 0;
	c->strut.right = 0;
	c->strut.top = 0;
	c->strut.bottom = 0;
	c->ncmapwins = 0;
	c->cmapwins = 0;
	c->wmcmaps = 0;
	c->accepts_focus = 1;
	c->next = clients;

	/* Add to head of list of clients. */
	clients = c;
	return clients;
}


void
Client_Remove(Client *c) {
	Client * cc;
	ScreenInfo *screen = c->screen;
	
	if (c == 0)
		return;
	
	/* Remove the client from our client list. */
	if (c == clients)
		clients = c->next;
	for (cc = clients; cc && cc->next; cc = cc->next) {
		if (cc->next == c)
			cc->next = cc->next->next;
	}
	
	/* Remove it from the hidden list if it's hidden. */
	if (hidden(c)) {
		unhidec(c, 0);
		/* Al Smith points out that you also want to get rid of the menu
		 * so you can be sure that if you let go on an item, you always
		 * get the corresponding window. */
		if (mode == wm_menu_up) {
			XUnmapWindow(dpy, current_screen->popup);
			mode = wm_idle;
		}
	}
	
	/* A deleted window can no longer be the current window. */
	if (c == current || (current == NULL && c == last_focus)) {
		Client *focus = NULL;

		/* As pointed out by J. Han, if a window disappears while it's
		 * being reshaped you need to get rid of the size indicator. */
		if (c == current && mode == wm_reshaping) {
			XUnmapWindow(dpy, current_screen->popup);
			mode = wm_idle;
		}
		if (focus_mode == focus_click) {
			/* Try and find the window that this was a transient
			 * for, else focus on the top client. */
			if (c->trans != None) {
				focus = Client_Get(c->trans);
			}
			if (!focus) {
				Window dw1;
				Window dw2;
				Window *wins;
				unsigned int nwins;

				XQueryTree(dpy, c->screen->root, &dw1,
					&dw2, &wins, &nwins);
				while (nwins) {
					focus = Client_Get(wins[nwins -1]);
					if (focus) break;
					nwins--;
				}
				if (wins) XFree(wins);
			}
		}
		Client_Focus(focus, CurrentTime);
	}
	
	if (getScreenFromRoot(c->parent) == 0)
		XDestroyWindow(dpy, c->parent);
	
	if (c->ncmapwins != 0) {
		XFree(c->cmapwins);
		free(c->wmcmaps);
	}
	
	if (c->name != 0)
		free(c->name);
	if (c->menu_name != 0)
		free(c->name);
	
	free(c);

	ewmh_set_client_list(screen);
	ewmh_set_strut(screen);
}


void
Client_MakeSane(Client *c, Edge edge, int *x, int *y, int *dx, int *dy) {
	Bool	horizontal_ok = True;
	Bool	vertical_ok = True;

	if (edge != ENone) {
		/*
		 *	Make sure we're not making the window too small.
		 */
		if (*dx < c->size.min_width)
			horizontal_ok = False;
		if (*dy < c->size.min_height)
			vertical_ok = False;

		/*
		 * Make sure we're not making the window too large.
		 */
		if (c->size.flags & PMaxSize) {
			if (*dx > c->size.max_width)
				horizontal_ok = False;
			if (*dy > c->size.max_height)
				vertical_ok = False;
		}

		/*
		 * Make sure the window's width & height are multiples of
		 * the width & height increments (not including the base size).
		 */

		if (c->size.width_inc > 1) {
			int apparent_dx = *dx - 2 * border - c->size.base_width;
			int x_fix = apparent_dx % c->size.width_inc;

			switch (edge) {
			case ELeft:
			case ETopLeft:
			case EBottomLeft:
				*x += x_fix;
				/*FALLTHROUGH*/
			case ERight:
			case ETopRight:
			case EBottomRight:
				*dx -= x_fix;
				break;
			default: break;
			}
		}

		if (c->size.height_inc > 1) {
			int apparent_dy = *dy - 2 * border - c->size.base_height;
			int y_fix = apparent_dy % c->size.height_inc;

			switch (edge) {
			case ETop:
			case ETopLeft:
			case ETopRight:
				*y += y_fix;
				/*FALLTHROUGH*/
			case EBottom:
			case EBottomLeft:
			case EBottomRight:
				*dy -= y_fix;
				break;
			default: break;
			}
		}

		/*
		 * Check that we may change the client horizontally and vertically.
		 */

		if (c->size.width_inc == 0)
			horizontal_ok = False;
		if (c->size.height_inc == 0)
			vertical_ok = False;
	}

	/* Ensure that at least one border is not entirely within the
	 * reserved areas. Keeping clients completely within the
	 * the workarea is too restrictive, but this measure means they
	 * should always be accessible.
	 * Of course all of this is only applicable if the client doesn't
	 * set a strut itself.					jfc
	 */
	if (c->strut.left == 0 && c->strut.right == 0 &&
		c->strut.top == 0 && c->strut.bottom == 0) {
		if ((int)(*y + border) >=
			(int)(c->screen->display_height -
			c->screen->strut.bottom)) {
			*y = c->screen->display_height -
				c->screen->strut.bottom -border;
		}
		if ((int)(*y + c->size.height - border) <=
			(int)c->screen->strut.top) {
			*y = c->screen->strut.top + border - c->size.height;
		}
		if ((int)(*x + border) >=
			(int)(c->screen->display_width -
			c->screen->strut.right)) {
			*x = c->screen->display_width -
				c->screen->strut.right -border;
		}
		if ((int)(*x + c->size.width - border) <=
			(int)c->screen->strut.left) {
			*x = c->screen->strut.left + border - c->size.width;
		}
	}

	/*
	 * Introduce a resistance to the workarea edge, so that windows may
	 * be "thrown" to the edge of the workarea without precise mousing,
	 * as requested by MAD.
	 */
	if (*x < (int)c->screen->strut.left &&
		*x > ((int)c->screen->strut.left - EDGE_RESIST)) {
		*x =  (int)c->screen->strut.left;
	}
	if ((*x + c->size.width) >
		(int)(c->screen->display_width - c->screen->strut.right) &&
		(*x + c->size.width) <
		(int)(c->screen->display_width - c->screen->strut.right +			EDGE_RESIST)) {
		*x = (int)(c->screen->display_width - c->screen->strut.right -
			c->size.width);
	}
	if ((*y - titleHeight()) < (int)c->screen->strut.top &&
		(*y - titleHeight()) >
		((int)c->screen->strut.top - EDGE_RESIST)) {
		*y =  (int)c->screen->strut.top + titleHeight();
	}
	if ((*y + c->size.height) >
		(int)(c->screen->display_height - c->screen->strut.bottom) &&
		(*y + c->size.height) <
		(int)(c->screen->display_height - c->screen->strut.bottom +			EDGE_RESIST)) {
		*y = (int)(c->screen->display_height - c->screen->strut.bottom -
			c->size.height);
	}

	/*
	 * Update that part of the client information that we're happy with.
	 */
	if (interacting_edge != ENone) {
		if (horizontal_ok) {
			c->size.x = *x;
			c->size.width  = *dx;
		}
		if (vertical_ok) {
			c->size.y = *y;
			c->size.height = *dy;
		}
	} else {
		if (horizontal_ok)
			c->size.x = *x;
		if (vertical_ok)
			c->size.y = *y;
	}
}

void
Client_SizeFeedback(void) {
	int x, y;
	char buf[4*2 + 3 + 1];

	/* Make the popup 10% wider than the widest string it needs to show. */
	snprintf(buf, sizeof(buf), "%i x %i", current_screen->display_width,
		current_screen->display_height);
	popup_width = popupWidth(buf, strlen(buf));
	popup_width += popup_width/10;

	/* Put the popup in the right place to report on the window's size. */
	getMousePosition(&x, &y);
	XMoveResizeWindow(dpy, current_screen->popup, x + 8, y + 8,
		popup_width, popupHeight() + 1);
	XMapRaised(dpy, current_screen->popup);
	
	/*
	* Ensure that the popup contents get redrawn. Eventually, the function
	* size_expose will get called to do the actual redraw.
	*/
	XClearArea(dpy, current_screen->popup, 0, 0, 0, 0, True);
}

void
size_expose(void) {
	int width, height;
	char buf[4*2 + 3 + 1];
	
	width = current->size.width - 2*border;
	height = current->size.height - 2*border;

	/* This dance ensures that we report 80x24 for an xterm even when
	 * it has a scrollbar. */
	if (current->size.flags & (PMinSize|PBaseSize) && current->size.flags & PResizeInc) {
		if (current->size.flags & PBaseSize) {
			width -= current->size.base_width;
			height -= current->size.base_height;
		} else {
			width -= current->size.min_width;
			height -= current->size.min_height;
		}
	}

	if (current->size.width_inc != 0)
		width /= current->size.width_inc;
	if (current->size.height_inc != 0)
		height /= current->size.height_inc;

	snprintf(buf, sizeof(buf), "%i x %i", width, height);
	XmbDrawString(dpy, current_screen->popup, popup_font_set,
		current_screen->size_gc,
		(popup_width - popupWidth(buf, strlen(buf))) / 2,
		ascent(popup_font_set_ext) + 1, buf, strlen(buf));
}

static void
Client_OpaquePrimitive(Client *c, Edge edge) {
	Cursor cursor;
	int ox, oy;

	if (c == 0 /*|| c != current*/)
		return;

	/* Find out where we've got hold of the window. */
	getMousePosition(&ox, &oy);
	ox = c->size.x - ox;
	oy = c->size.y - oy;
	
	cursor = getEdgeCursor(edge);
	XChangeActivePointerGrab(dpy, ButtonMask | PointerMotionHintMask |
		ButtonMotionMask | OwnerGrabButtonMask, cursor, CurrentTime);
	
	/*
	 * Store some state so that we can get back into the main event
	 * dispatching thing.
	 */
	interacting_edge = edge;
	start_x = ox;
	start_y = oy;
	mode = wm_reshaping;
	ewmh_set_client_list(c->screen);
}

void
Client_Lower(Client *c)
{
	if (c == 0) return;

	XLowerWindow(dpy, c->window);
	if (c->framed) XLowerWindow(dpy, c->parent);
	ewmh_set_client_list(c->screen);
}

void
Client_Raise(Client *c)
{
	Client * trans;

	if (c == 0) return;

	if (c->framed) XRaiseWindow(dpy, c->parent);
	XRaiseWindow(dpy, c->window);

	for (trans = clients; trans != NULL; trans = trans->next) {
		if (trans->trans != c->window &&
			!(c->framed == True && trans->trans == c->parent))
			continue;
		if (trans->framed) XRaiseWindow(dpy, trans->parent);
		XRaiseWindow(dpy, trans->window);
	}
	
	ewmh_set_client_list(c->screen);
}

void
Client_Close(Client *c) {
	if (c == 0)
		return;

	/*
	 *	Terminate the client nicely if possible. Be brutal otherwise.
	 */
	if (c->proto & Pdelete) {
		sendClientMessage(c->window, wm_protocols, wm_delete, CurrentTime);
	} else {
		XKillClient(dpy, c->window);
	}
}

void
Client_SetState(Client *c, int state) {
	long	data[2];

	data[0] = (long) state;
	data[1] = (long) None;

	c->state = state;
	XChangeProperty(dpy, c->window, wm_state, wm_state, 32,
		PropModeReplace, (unsigned char *) data, 2);
	ewmh_set_state(c);
}

static void
sendClientMessage(Window w, Atom a, long data0, long data1) {
	XEvent	ev;
	long	mask;

	memset(&ev, 0, sizeof(ev));
	ev.xclient.type = ClientMessage;
	ev.xclient.window = w;
	ev.xclient.message_type = a;
	ev.xclient.format = 32;
	ev.xclient.data.l[0] = data0;
	ev.xclient.data.l[1] = data1;
	mask = (getScreenFromRoot(w) != 0) ? SubstructureRedirectMask : 0L;

	XSendEvent(dpy, w, False, mask, &ev);
}

extern void
Client_ResetAllCursors(void) {
	Client *c;
	XSetWindowAttributes attr;

	for (c = clients; c; c = c->next) {
		if (c->framed != True) continue;
		attr.cursor = c->screen->root_cursor;
		XChangeWindowAttributes(dpy, c->parent,
			CWCursor, &attr);
		c->cursor = ENone;
	}
}

extern void
Client_FreeAll(void) {
	Client *c;
	XWindowChanges wc;

	for (c = clients; c; c = c->next) {
		int not_mapped = !normal(c);

		/* elliott thinks leaving window unmapped causes the x server
		 * to lose them when the window manager quits. it doesn't
		 * happen to me with XFree86's Xnest, but unmapping the
		 * windows stops gtk window generating an extra window when
		 * the window manager quits.
		 * who is right? only time will tell....
		 */
		XUnmapWindow(dpy, c->parent);
		XUnmapWindow(dpy, c->window);
		/* Remap the window if it's hidden.  
		if (not_mapped) {
			XMapWindow(dpy, c->parent);
			XMapWindow(dpy, c->window);
		} */

		/* Reparent it, and then push it to the bottom if it was hidden. */
		XReparentWindow(dpy, c->window, c->screen->root, c->size.x, c->size.y);
		if (not_mapped)
			XLowerWindow(dpy, c->window);

		/* Give it back its initial border width. */
		wc.border_width = c->border;
		XConfigureWindow(dpy, c->window, CWBorderWidth, &wc);
	}
}

extern void
Client_ColourMap(XEvent *e) {
	int i;
	Client * c;

	for (c = clients; c; c = c->next) {
		for (i = 0; i < c->ncmapwins; i++) {
			if (c->cmapwins[i] == e->xcolormap.window) {
				c->wmcmaps[i] = e->xcolormap.colormap;
				if (c == current)
					cmapfocus(c);
				return;
			}
		}
	}
}

extern void
Client_ReshapeEdge(Client *c, Edge e) {
	Client_OpaquePrimitive(c, e);
}

extern void
Client_Move(Client *c) {
	Client_OpaquePrimitive(c, ENone);
}

extern int
hidden(Client *c) {
	return c->state == IconicState;
}

extern int
withdrawn(Client *c) {
	return c->state == WithdrawnState;
}

extern int
normal(Client *c) {
	return c->state == NormalState;
}

extern void
Client_EnterFullScreen(Client *c) {
	XWindowChanges fs;

	memcpy(&c->return_size, &c->size, sizeof(XSizeHints));
	if (c->framed) {
		c->size.x = fs.x = -border;
		c->size.y = fs.y = -border;
		c->size.width = fs.width =
			c->screen->display_width + 2 * border;
		c->size.height = fs.height =
			c->screen->display_height + titleHeight() + 2 * border;
		XConfigureWindow(dpy, c->parent,
			CWX | CWY | CWWidth | CWHeight, &fs);

		fs.x = border;
		fs.y = border;
		fs.width = c->screen->display_width;
		fs.height = c->screen->display_height;
		XConfigureWindow(dpy, c->window,
			CWX | CWY | CWWidth | CWHeight, &fs);
		XRaiseWindow(dpy,c->parent);
	} else {
		c->size.x = c->size.y = fs.x = fs.y = 0;
		c->size.width = fs.width = c->screen->display_width;
		c->size.height = fs.height = c->screen->display_height;
		XConfigureWindow(dpy, c->window,
			CWX | CWY | CWWidth | CWHeight, &fs);
		XRaiseWindow(dpy,c->window);
	}
	sendConfigureNotify(c);
}

extern void
Client_ExitFullScreen(Client *c) {
	XWindowChanges fs;

	memcpy(&c->size, &c->return_size, sizeof(XSizeHints));
	if (c->framed == True) {
		fs.x = c->size.x;
		fs.y = c->size.y - titleHeight();
		fs.width = c->size.width;
		fs.height = c->size.height + titleHeight();
		XConfigureWindow(dpy, c->parent,
			CWX | CWY | CWWidth | CWHeight, &fs);

		fs.x = border;
		fs.y = border + titleHeight();
		fs.width = c->size.width -(2 * border);
		fs.height = c->size.height -(2 * border);
		XConfigureWindow(dpy, c->window,
			CWX | CWY | CWWidth | CWHeight, &fs);
	} else {
		fs.x = c->size.x;
		fs.y = c->size.y;
		fs.width = c->size.width;
		fs.height = c->size.height;
		XConfigureWindow(dpy, c->window,
			CWX | CWY | CWWidth | CWHeight, &fs);
	}
	sendConfigureNotify(c);
}

extern void
Client_Focus(Client *c, Time time) {
	if (current) {
		setactive(current, 0, 0L);
		XDeleteProperty(dpy, current->screen->root,
			ewmh_atom[_NET_ACTIVE_WINDOW]);
	}

	if (!c && current) {
		last_focus = current;
	} else {
		last_focus = NULL;
	}
	current = c;
	if (c) {
		setactive(current, 1, time);
		XChangeProperty(dpy, current->screen->root,
			ewmh_atom[_NET_ACTIVE_WINDOW],
			XA_WINDOW, 32, PropModeReplace,
			(unsigned char *)&current->window, 1);
	}

	if (focus_mode == focus_click)
		Client_Raise(c);
}

extern void
Client_Name(Client *c, const char *name, Bool is_utf8) {
	int tx;
	static const char dots[] = " [...] ";
	int cut;

	if (c->name) free(c->name);
	c->name = sdup((char *) name);
	c->namelen = strlen(c->name);
	c->name_utf8 = is_utf8;

	if (c->menu_name) free(c->menu_name);
	c->menu_name = 0;
	tx = titleWidth(popup_font_set, c);
	if (tx <= (c->screen->display_width - (c->screen->display_width / 10)))
		return;

	/* the menu entry for this client will not fit on the display
	 * (minus 10% for saftey), so produced a truncated version...
	 */
	cut = 5;
	do {
		if (c->menu_name) {
			free(c->menu_name);
			c->menu_name = 0;
		}
		if (cut >= (strlen(c->name) / 2)) break;
		c->menu_name = sdup(c->name);
		/* FIXME: this is not UTF-8 safe! */
		sprintf(&c->menu_name[(strlen(c->name) / 2) - cut], dots);
		strcat(c->menu_name,
			&c->name[(strlen(c->name) / 2) + cut]);
		c->menu_namelen = strlen(c->menu_name);
		cut++;
		tx = titleWidth(popup_font_set, c);
		if (!tx) break;
	} while (tx >
		(c->screen->display_width - (c->screen->display_width / 10)));
}
