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

#include <X11/X.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/Xos.h>
#include <X11/Xresource.h>
#include <X11/Xatom.h>

#include <stdio.h>
#include <stdlib.h>

#include "lwm.h"
#include "ewmh.h"

/*
 *    Dispatcher for main event loop.
 */
typedef struct Disp Disp;
struct Disp {
	int	type;
	void	(*handler)(XEvent *);
};

static void expose(XEvent *);
static void buttonpress(XEvent *);
static void buttonrelease(XEvent *);
static void focuschange(XEvent *);
static void maprequest(XEvent *);
static void configurereq(XEvent *);
static void unmap(XEvent *);
static void destroy(XEvent *);
static void clientmessage(XEvent *);
static void colormap(XEvent *);
static void property(XEvent *);
static void reparent(XEvent *);
static void enter(XEvent *);
static void circulaterequest(XEvent *);
static void motionnotify(XEvent *);

void reshaping_motionnotify(XEvent *);

static Disp disps[] =
{
	{Expose, expose},
	{MotionNotify, motionnotify},
	{ButtonPress, buttonpress},
	{ButtonRelease, buttonrelease},
	{FocusIn, focuschange},
	{FocusOut, focuschange},
	{MapRequest, maprequest},
	{ConfigureRequest, configurereq},
	{UnmapNotify, unmap},
	{DestroyNotify, destroy},
	{ClientMessage, clientmessage},
	{ColormapNotify, colormap},
	{PropertyNotify, property},
	{ReparentNotify, reparent},
	{EnterNotify, enter},
	{CirculateRequest, circulaterequest},
	{LeaveNotify, 0},
	{ConfigureNotify, 0},
	{CreateNotify, 0},
	{GravityNotify, 0},
	{MapNotify, 0},
	{MappingNotify, 0},
	{SelectionClear, 0},
	{SelectionNotify, 0},
	{SelectionRequest, 0},
	{NoExpose, 0},
};

/**
 * pending it the client in which an action has been started by a mouse press
 * and we are waiting for the button to be released before performing the action
 */
static Client *pending=NULL;

extern void
dispatch(XEvent * ev) {
	Disp * p;

	for (p = disps; p < disps + sizeof(disps)/sizeof(disps[0]); p++) {
		if (p->type == ev->type) {
			if (p->handler != 0)
				p->handler(ev);
			return;
		}
	}

	if (!shapeEvent(ev))
		fprintf(stderr, "%s: unknown event %d\n", argv0, ev->type);
}

static void
expose(XEvent * ev) {
	Client * c;
	Window w;	/* Window the expose event is for. */

	/* Only handle the last in a group of Expose events. */
	if (ev->xexpose.count != 0) return;

	w = ev->xexpose.window;

	/*
	* We don't draw on the root window so that people can have
	* their favourite Spice Girls backdrop...
	*/
	if (getScreenFromRoot(w) != 0)
		return;

	/* Decide what needs redrawing: window frame or menu? */
	if (current_screen && w == current_screen->popup) {
		if (mode == wm_menu_up)
			menu_expose();
		else if (mode == wm_reshaping && current != 0)
			size_expose();
	} else {
		c = Client_Get(w);
		if (c != 0) {
			Client_DrawBorder(c, c == current);
		}
	}
}

static void
buttonpress(XEvent *ev) {
	Client *c;
	XButtonEvent *e = &ev->xbutton;
	int quarter;

	/* If we're getting it already, we're not in the market for more. */
	if (mode != wm_idle) {
		/* but allow a button press to cancel a move/resize,
		 * to satify the EWMH advisory to allow a second mechanism
		 * of completing move/resize operations, due to a race.
		 * (section 4.3) sucky!
		 */
		if (mode == wm_reshaping) {
			mode = wm_idle;
		}
		return;
	}

	c = Client_Get(e->window);

	if (c && c != current && focus_mode == focus_click) {
		/* Click is not on current window,
		 * and in click-to-focus mode, so change focus
		 */
		Client_Focus(c, e->time);
	}
	
	/*move this test up to disable scroll to focus*/
	if (e->button >= 4 && e->button <= 7) {
		return;
	}
	
	if (c && c == current && (e->window == c->parent)) {
		/* Click went to our frame around a client. */

		/* The ``box''. */
		quarter = (border + titleHeight()) / 4;
		if (e->x > (quarter + 2) && e->x < (3 + 3*quarter) && e->y > quarter && e->y <= 3*quarter) {
			/*Client_Close(c);*/
			pending = c;
			mode = wm_closing_window;
			return;
		}

		/* Somewhere in the rest of the frame. */
		if (e->button == HIDE_BUTTON) {
			pending = c;
			mode = wm_hiding_window;
			return;
		}
		if (e->button == MOVE_BUTTON) {
			Client_Move(c);
			return;
		}		
		if (e->button == RESHAPE_BUTTON) {
			XMapWindow(dpy, c->parent);
			Client_Raise(c);

			/* Lasciate ogni speranza voi ch'entrate...  */

			if (e->x <= border && e->y <= border) {
				Client_ReshapeEdge(c, ETopLeft);
			} else if (e->x >= (c->size.width - border) && e->y <= border) {
				Client_ReshapeEdge(c, ETopRight);
			} else if (e->x >= (c->size.width - border) && e->y >= (c->size.height + titleHeight() - border)) {
				Client_ReshapeEdge(c, EBottomRight);
			} else if (e->x <= border && e->y >= (c->size.height + titleHeight() - border)) {
				Client_ReshapeEdge(c, EBottomLeft);
			} else if (e->x > border && e->x < (c->size.width - border) && e->y < border) {
				Client_ReshapeEdge(c, ETop);
			} else if (e->x > border && e->x < (c->size.width - border) && e->y >= border && e->y < (titleHeight() + border)) {
				Client_Move(c);
			} else if (e->x > (c->size.width - border) && e->y > border && e->y < (c->size.height + titleHeight() - border)) {
				Client_ReshapeEdge(c, ERight);
			} else if (e->x > border && e->x < (c->size.width - border) && e->y > (c->size.height - border)) {
				Client_ReshapeEdge(c, EBottom);
			} else if (e->x < border && e->y > border && e->y < (c->size.height + titleHeight() - border)) {
				Client_ReshapeEdge(c, ELeft);
			}
			return;
		}
		return;
	}

	/* Deal with root window button presses. */
	if (e->window == e->root) {
		if (e->button == Button3) {
			cmapfocus(0);
			menuhit(e);
		} else {
			shell(getScreenFromRoot(e->root), e->button, e->x, e->y);
		}
	}
}

static void
buttonrelease(XEvent *ev) {
	XButtonEvent *e = &ev->xbutton;
	int quarter;

	if (mode == wm_menu_up)
		menu_buttonrelease(ev);
	else if (mode == wm_reshaping)
		XUnmapWindow(dpy, current_screen->popup);
	else if (mode == wm_closing_window) {
		/* was the button released within the window's box?*/
		quarter = (border + titleHeight()) / 4;
		if (pending != NULL &&
			(e->window == pending->parent) &&
			(e->x > (quarter + 2) &&
				e->x < (3 + 3*quarter) &&
				e->y > quarter && e->y <= 3*quarter))
					Client_Close(pending);
		pending = NULL;
	} else if (mode == wm_hiding_window) {
		/* was the button release within the window's frame? */
		if (pending != NULL &&
			(e->window == pending->parent) &&
			(e->x >= 0) && (e->y >= 0) &&
			(e->x <= pending->size.width) &&
			(e->y <= (pending->size.height + titleHeight()))) {
			if (e->state & ShiftMask) {
				Client_Lower(pending);
			} else {
				hide(pending);
			}
		}
		pending = NULL;
	}
	
	mode = wm_idle;
}

static void circulaterequest(XEvent *ev) {
	XCirculateRequestEvent * e = &ev->xcirculaterequest;
	Client * c;
	
	c = Client_Get(e->window);

	if (c == 0) {
		if (e->place == PlaceOnTop) {
			XRaiseWindow(e->display, e->window);
		} else {
			XLowerWindow(e->display, e->window);
		}
	} else {
		if (e->place == PlaceOnTop) {
			Client_Raise(c);
		} else {
			Client_Lower(c);
		}
	}
}

static void
maprequest(XEvent *ev) {
	Client * c;
	XMapRequestEvent * e = &ev->xmaprequest;
	
	c = Client_Get(e->window);
	
	if (c == 0 || c->window != e->window) {
		int screen;
		for (screen = 0; screen < screen_count; screen++)
			scanWindowTree(screen);
		c = Client_Get(e->window);
		if (c == 0 || c->window != e->window) {
			fprintf(stderr, "MapRequest for non-existent window!\n");
			return;
		}
	}
	
	unhidec(c, 1);
	
	switch (c->state) {
	case WithdrawnState:
		if (getScreenFromRoot(c->parent) != 0) {
			manage(c, 0);
			break;
		}
		if (c->framed == True) {
			XReparentWindow(dpy, c->window, c->parent, border,
				border + titleHeight());
		} else {
			XReparentWindow(dpy, c->window, c->parent,
				c->size.x, c->size.y);
		}
		XAddToSaveSet(dpy, c->window);
		/*FALLTHROUGH*/
	case NormalState:
		XMapWindow(dpy, c->parent);
		XMapWindow(dpy, c->window);
		Client_Raise(c);
		Client_SetState(c, NormalState);
		break;
	}
	ewmh_set_client_list(c->screen);
}

static void
unmap(XEvent *ev) {
	Client *c;
	XUnmapEvent *e = &ev->xunmap;

	c = Client_Get(e->window);
	if (c == 0) return;

	/*
	 * In the description of the ReparentWindow request we read: "If the window
	 * is mapped, an UnmapWindow request is performed automatically first". This
	 * might seem stupid, but it's the way it is. While a reparenting is pending
	 * we ignore UnmapWindow requests.
	 */
	if (c->internal_state == IPendingReparenting) {
		c->internal_state = INormal;
		return;
	}

	/* "This time it's the real thing." */

	if (c->state == IconicState) {
		/*
		 * Is this a hidden window disappearing? If not, then we
		 * aren't interested because it's an unmap request caused
		 * by our hiding a window.
		 */
		if (e->send_event)
			unhidec(c, 0); /* It's a hidden window disappearing. */
	} else {
		/* This is a plain unmap, so withdraw the window. */
		withdraw(c);
	}

	c->internal_state = INormal;
}

static void
configurereq(XEvent *ev) {
	XWindowChanges wc;
	Client *c;
	XConfigureRequestEvent *e = &ev->xconfigurerequest;
	
	c = Client_Get(e->window);


	if (c && c->window == e->window) {
		/*
		* ICCCM section 4.1.5 says that the x and y coordinates here
		* will have been "adjusted for the border width".
		* NOTE: this may not be the only place to bear this in mind.
		*/
		if (e->value_mask & CWBorderWidth) {
			e->x -= e->border_width;
			e->y -= e->border_width;
		} else {
			/*
			* The ICCCM also says that clients should always set the
			* border width in a configure request. As usual, many don't.
			*/
			/* adding one seems a bit arbitrary and makes edit
				drift by one pixel*/
			/*e->x--;*/
			/*e->y--;*/
		}

		if (e->value_mask & CWX) {
			c->size.x = e->x;
		}
		if (e->value_mask & CWY) {
			c->size.y = e->y;
			if (c->framed == True) 
				c->size.y += titleHeight();
		}
		if (e->value_mask & CWWidth) {
			c->size.width = e->width;
			if (c->framed == True) 
				c->size.width += 2 * border;
		}
		if (e->value_mask & CWHeight) {
			c->size.height = e->height;
			if (c->framed == True) 
				c->size.height += 2 * border;
		}
		if (e->value_mask & CWBorderWidth)
			c->border = e->border_width;

		if (getScreenFromRoot(c->parent) == 0) {
			wc.x = c->size.x;
			wc.y = c->size.y;
			if (c->framed == True) 
				wc.y -= titleHeight();
			wc.width = c->size.width;
			wc.height = c->size.height;
			if (c->framed == True) 
				wc.height += titleHeight();
			wc.border_width = 1;
			wc.sibling = e->above;
			wc.stack_mode = e->detail;
			
			XConfigureWindow(dpy, e->parent, e->value_mask, &wc);
			sendConfigureNotify(c);
		}
	}
	if (c && (c->internal_state == INormal) && (c->framed == True)) {
		wc.x = border;
		wc.y = border;
	} else {
		wc.x = e->x;
		wc.y = e->y;
	}

	wc.width = e->width;
	wc.height = e->height;
	wc.border_width = 0;
	wc.sibling = e->above;
	wc.stack_mode = e->detail;
	e->value_mask |= CWBorderWidth;
	
	XConfigureWindow(dpy, e->window, e->value_mask, &wc);
	
	if (c) {
		if (c->framed == True)  {
			XMoveResizeWindow(dpy, c->parent,
				c->size.x, c->size.y - titleHeight(),
				c->size.width, c->size.height + titleHeight());
			XMoveWindow(dpy, c->window,
				border, border + titleHeight());
		} else {
			XMoveResizeWindow(dpy, c->window,
				c->size.x, c->size.y,
				c->size.width, c->size.height);
		}
	}
}

static void
destroy(XEvent *ev) {
	Client * c;
	Window w = ev->xdestroywindow.window;

	c = Client_Get(w);
	if (c == 0)
		return;

	ignore_badwindow = 1;
	Client_Remove(c);
	ignore_badwindow = 0;
}

static void
clientmessage(XEvent *ev) {
	Client * c;
	XClientMessageEvent * e = &ev->xclient;

	c = Client_Get(e->window);
	if (c == 0) return;
	if (e->message_type == wm_change_state) {
		if (e->format == 32 && e->data.l[0] == IconicState && normal(c))
			hide(c);
		return;
	}
	if (e->message_type == ewmh_atom[_NET_WM_STATE] &&
		e->format == 32) {
		ewmh_change_state(c, e->data.l[0], e->data.l[1]);
		ewmh_change_state(c, e->data.l[0], e->data.l[2]);
		return;
	}
	if (e->message_type == ewmh_atom[_NET_ACTIVE_WINDOW] &&
		e->format == 32) {
		/* An EWMH enabled application has asked for this client
		 * to be made the active window. The window is raised, and
		 * focus given if the focus mode is click (focusing on a
		 * window other than the one the pointer is in makes no
		 * sense when the focus mode is enter).
		 */
		if (hidden(c)) unhidec(c,1);
		XMapWindow(dpy, c->parent);
		Client_Raise(c);
		if (c != current && focus_mode == focus_click)
			Client_Focus(c, CurrentTime);
		return;
	}
	if (e->message_type == ewmh_atom[_NET_CLOSE_WINDOW] &&
		e->format == 32) {
		Client_Close(c);
		return;
	}
	if (e->message_type == ewmh_atom[_NET_MOVERESIZE_WINDOW] &&
		e->format == 32) {
		XEvent ev;

		/* FIXME: ok, so this is a bit of a hack */
		ev.xconfigurerequest.window = e->window;
		ev.xconfigurerequest.x = e->data.l[1];
		ev.xconfigurerequest.y = e->data.l[2];
		ev.xconfigurerequest.width = e->data.l[3];
		ev.xconfigurerequest.height = e->data.l[4];
		ev.xconfigurerequest.value_mask = 0;
		if (e->data.l[0] & (1 << 8))
			ev.xconfigurerequest.value_mask |= CWX;
		if (e->data.l[0] & (1 << 9))
			ev.xconfigurerequest.value_mask |= CWY;
		if (e->data.l[0] & (1 << 10))
			ev.xconfigurerequest.value_mask |= CWWidth;
		if (e->data.l[0] & (1 << 11))
			ev.xconfigurerequest.value_mask |= CWHeight;
		configurereq(&ev);
		return;
	}
	if (e->message_type == ewmh_atom[_NET_WM_MOVERESIZE] &&
		e->format == 32) {
		Edge edge = E_LAST;
		EWMHDirection direction = e->data.l[2];
/*
		int x_root = e->data.l[0];
		int y_root = e->data.l[1];
*/

		
		/* before we can do any resizing, make the window visible */
		if (hidden(c)) unhidec(c,1);
		XMapWindow(dpy, c->parent);
		Client_Raise(c);
		/* FIXME: we're ignorning x_root, y_root and button! */
		switch (direction) {
		case DSizeTopLeft:
			edge = ETopLeft;
			break;
		case DSizeTop:
			edge = ETop;
			break;
		case DSizeTopRight:
			edge = ETopRight;
			break;
		case DSizeRight:
			edge = ERight;
			break;
		case DSizeBottomRight:
			edge = EBottomRight;
			break;
		case DSizeBottom:
			edge = EBottom;
			break;
		case DSizeBottomLeft:
			edge = EBottomLeft;
			break;
		case DSizeLeft:
			edge = ELeft;
			break;
		case DMove:
			edge = ENone;
			break;
		case DSizeKeyboard:
			/* FIXME: don't know how to deal with this */
			edge = E_LAST;
			break;
		case DMoveKeyboard:
#if 0
/* need to do a release and this is too broken for that */
			/* don't believe i'm doing this. mouse warping
			 * sucks!
			 */
			XWarpPointer(dpy, c->screen->root, c->window,
				x_root, y_root,
				c->screen->display_width,
				c->screen->display_height,
				c->size.width / 2, c->size.height / 2);
			edge = ENone;
#endif
			edge = E_LAST;
			break;
		default:
			edge = E_LAST;
			fprintf(stderr, "%s: received _NET_WM_MOVERESIZE"
				" with bad direction", argv0);
			break;
		}
		switch (edge) {
		case E_LAST:
			break;
		case ENone:
			Client_Move(c);
			break;
		default:
			Client_ReshapeEdge(c, edge);
			break;
		}
	}
}

static void
colormap(XEvent *ev) {
	Client * c;
	XColormapEvent * e = &ev->xcolormap;
	
	if (e->new) {
		c = Client_Get(e->window);
		if (c) {
			c->cmap = e->colormap;
			if (c == current)
				cmapfocus(c);
		} else {
			Client_ColourMap(ev);
		}
	}
}

static void
property(XEvent * ev) {
	Client * c;
	XPropertyEvent * e = &ev->xproperty;
	
	c = Client_Get(e->window);
	if (c == 0)
		return;
	
	if (e->atom == _mozilla_url || e->atom == XA_WM_NAME) {
		getWindowName(c);
		setactive(c, c == current, 0L);
	} else if (e->atom == XA_WM_TRANSIENT_FOR) {
		getTransientFor(c);
	} else if (e->atom == XA_WM_NORMAL_HINTS) {
		getNormalHints(c);
	} else if (e->atom == wm_colormaps) {
		getColourmaps(c);
		if (c == current)
			cmapfocus(c);
	} else if (e->atom == ewmh_atom[_NET_WM_STRUT]) {
		ewmh_get_strut(c);
	}
}

static void
reparent(XEvent *ev) {
	Client * c;
	XReparentEvent * e = &ev->xreparent;
	
	if (getScreenFromRoot(e->event) == 0 || e->override_redirect || getScreenFromRoot(e->parent) != 0)
		return;
	
	c = Client_Get(e->window);
	if (c != 0 && (getScreenFromRoot(c->parent) != 0 || withdrawn(c)))
		Client_Remove(c);
}

static void
focuschange(XEvent *ev) {
	Client *c;
	Window focus_window;
	int revert_to;

	if (ev->type == FocusOut) return;

	XGetInputFocus(dpy, &focus_window, &revert_to);
	if (focus_window == PointerRoot || focus_window == None) {
		if (current) Client_Focus(NULL, CurrentTime);
		return;
	}
	c = Client_Get(focus_window);
	if (c && c != current) {
		Client_Focus(c, CurrentTime);
	}
	return;
}

static void
enter(XEvent *ev) {
	Client *c;

	c = Client_Get(ev->xcrossing.window);
	if (c == 0 || mode != wm_idle)
		return;

	if (c->framed == True) {
		XSetWindowAttributes attr;

		attr.cursor = c->screen->root_cursor;
		XChangeWindowAttributes(dpy, c->parent,
			CWCursor, &attr);
		c->cursor = ENone;
	}
	if (c != current && !c->hidden && focus_mode == focus_enter) {
		/* Entering a new window in enter focus mode, so take focus */
		Client_Focus(c, ev->xcrossing.time);
	}
}

static void
motionnotify(XEvent *ev) {
	if (mode == wm_reshaping)
		reshaping_motionnotify(ev);
	else if (mode == wm_menu_up)
		menu_motionnotify(ev);
	else if (mode == wm_idle) {
		XMotionEvent *e = &ev->xmotion;
		Client *c = Client_Get(e->window);
		Edge edge = ENone;
		int quarter = (border + titleHeight()) / 4;

		if (c && (e->window == c->parent) &&
			(e->subwindow != c->window) &&
			mode == wm_idle) {
			/* mouse moved in a frame we manage - check cursor */
			if (e->x > (quarter + 2)
				&& e->x < (3 + 3*quarter)
				&& e->y > quarter && e->y <= 3*quarter) {
				edge = E_LAST;
			} else if (e->x <= border && e->y <= border) {
				edge = ETopLeft;
			} else if (e->x >= (c->size.width - border)
				&& e->y <= border) {
				edge = ETopRight;
			} else if (e->x >= (c->size.width - border)
				&& e->y >=
				(c->size.height + titleHeight() - border)) {
				edge = EBottomRight;
			} else if (e->x <= border &&
				e->y >=
				(c->size.height + titleHeight() - border)) {
				edge = EBottomLeft;
			} else if (e->x > border &&
				e->x < (c->size.width - border)
				&& e->y < border) {
				edge = ETop;
			} else if (e->x > border &&
				e->x < (c->size.width - border)
				&& e->y >= border
				&& e->y < (titleHeight() + border)) {
				edge = ENone;
			} else if (e->x > (c->size.width - border)
				&& e->y > border
				&& e->y <
				(c->size.height + titleHeight() - border)) {
				edge = ERight;
			} else if (e->x > border
				&& e->x < (c->size.width - border)
				&& e->y > (c->size.height - border)) {
				edge = EBottom;
			} else if (e->x < border
				&& e->y > border
				&& e->y <
				(c->size.height + titleHeight() - border)) {
				edge = ELeft;
			}
			if (c->cursor != edge) {
				XSetWindowAttributes attr;

				if (edge == ENone) {
					attr.cursor =
						c->screen->root_cursor;
				} else if (edge == E_LAST) {
					attr.cursor =
						c->screen->box_cursor;
				} else {
					attr.cursor =
						c->screen->cursor_map[edge];
				}
				XChangeWindowAttributes(dpy, c->parent,
					CWCursor, &attr);
				c->cursor = edge;
			}
		}
	}
}

/*ARGSUSED*/
void
reshaping_motionnotify(XEvent* ev) {
	int	nx;	/* New x. */
	int	ny;	/* New y. */
	int	ox;	/* Original x. */
	int	oy;	/* Original y. */
	int	ndx;	/* New width. */
	int	ndy;	/* New height. */
	int	odx;	/* Original width. */
	int	ody;	/* Original height. */
	int	pointer_x;
	int	pointer_y;

	if (mode != wm_reshaping || !current) return;

	getMousePosition(&pointer_x, &pointer_y);

	if (interacting_edge != ENone) {
		nx = ox = current->size.x;
		ny = oy = current->size.y;
		ndx = odx = current->size.width;
		ndy = ody = current->size.height;
		
		Client_SizeFeedback();

		/* Vertical. */
		switch (interacting_edge) {
		case ETop:
		case ETopLeft:
		case ETopRight:
			pointer_y += titleHeight();
			ndy += (current->size.y - pointer_y);
			ny = pointer_y;
			break;
		case EBottom:
		case EBottomLeft:
		case EBottomRight:
			ndy = pointer_y - current->size.y;
			break;
		default:	break;
		}

		/* Horizontal. */
		switch (interacting_edge) {
		case ERight:
		case ETopRight:
		case EBottomRight:
			ndx = pointer_x - current->size.x;
			break;
		case ELeft:
		case ETopLeft:
		case EBottomLeft:
			ndx += (current->size.x - pointer_x);
			nx = pointer_x;
			break;
		default: break;
		}

		Client_MakeSane(current, interacting_edge, &nx, &ny, &ndx, &ndy);
		XMoveResizeWindow(dpy, current->parent,
			current->size.x, current->size.y - titleHeight(),
			current->size.width, current->size.height + titleHeight());
		if (current->size.width == odx && current->size.height == ody) {
			if (current->size.x != ox || current->size.y != oy)
				sendConfigureNotify(current);
		} else
			XMoveResizeWindow(dpy, current->window,
				border, border + titleHeight(),
				current->size.width - 2 * border,
				current->size.height - 2 * border);
	} else {
		nx = pointer_x + start_x;
		ny = pointer_y + start_y;

		Client_MakeSane(current, interacting_edge, &nx, &ny, 0, 0);
		if (current->framed == True) {
			XMoveWindow(dpy, current->parent,
				current->size.x,
				current->size.y - titleHeight());
		} else {
			XMoveWindow(dpy, current->parent,
				current->size.x, current->size.y);
		}
		sendConfigureNotify(current);
	}
}
