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

#include <X11/X.h>
#include <X11/Xos.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>

#include "lwm.h"
#include "ewmh.h"

static int current_item;	/* Last known selected menu item. -1 if none. */

typedef struct menuitem menuitem;
struct menuitem {
	Client * client;
	menuitem * next;
};

static menuitem * hidden_menu = 0;

static void getMenuDimensions(int *, int *, int *);

void
getMousePosition(int * x, int * y) {
	Window root, child;
	int t1, t2;
	unsigned int b;
	
	/* It doesn't matter which root window we give this call. */
	XQueryPointer(dpy, screens[0].root, &root, &child, x, y, &t1, &t2, &b);
	current_screen = getScreenFromRoot(root);
}

int
menu_whichitem(int x, int y) {
	int width;	/* Width of menu. */
	int height;	/* Height of each menu item. */
	int length;	/* Number of items on the menu. */
	
	getMenuDimensions(&width, &height, &length);
	
	/*
	 * Translate to popup window coordinates. We do this ourselves to avoid
	 * a round trip to the server.
	 */
	x -= start_x;
	y -= start_y;
	
	/*
	 * Are we outside the menu?
	 */
	if (x < 0 || x > width || y < 0 || y >= length * height)
		return -1;
	
	return y / height;
}

static void
getMenuDimensions(int *width, int *height, int *length) {
	int w;	/* Widest string so far. */
	int i;	/* Menu item. */
	
	menuitem *m = hidden_menu;
	
	w = 0;
	for (i = 0; m != 0; m = m->next, i++) {
		int tw = titleWidth(popup_font_set, m->client) + 4;
		if (tw > w) w = tw;
	}
	
	*width = w + border;
	*height = popupHeight();
	*length = i;
}

void
menuhit(XButtonEvent *e) {
	int width;	/* Width of menu. */
	int height;	/* Height of each menu item. */
	int length;	/* Number of menu items. */
	
	if (hidden_menu == 0)
		return;

	Client_ResetAllCursors();
	
	current_screen = getScreenFromRoot(e->root);
	
	getMenuDimensions(&width, &height, &length);
	
	/*
	 * Arrange for centre of first menu item to be under pointer,
	 * unless that would put the menu offscreen.
	 */
	start_x = e->x - width / 2;
	start_y = e->y - height / 2;
	
	if (start_x + width > current_screen->display_width)
		start_x = current_screen->display_width - width;
	if (start_x < 0)
		start_x = 0;
	if (start_y + (height * length) > current_screen->display_height)
		start_y = current_screen->display_height - (height * length);
	if (start_y < 0)
		start_y = 0;

	
	current_item = menu_whichitem(e->x_root, e->y_root);
	
	XMoveResizeWindow(dpy, current_screen->popup, start_x, start_y,
		width, length * height);
	XMapRaised(dpy, current_screen->popup);
	XChangeActivePointerGrab(dpy, ButtonMask | ButtonMotionMask |
		OwnerGrabButtonMask, None, CurrentTime);
	
	mode = wm_menu_up;
}

void 
hide(Client *c) {
	menuitem *newitem;

	if (c == 0)
		return;

	/* Create new menu item, and thread it on the menu. */
	newitem = (menuitem *) malloc(sizeof(menuitem));
	if (newitem == 0)
		return;
	newitem->client = c;
	newitem->next = hidden_menu;
	hidden_menu = newitem;

	/* Actually hide the window. */
	XUnmapWindow(dpy, c->parent);
	XUnmapWindow(dpy, c->window);

	c->hidden = True;

	/* If the window was the current window, it isn't any more... */
	if (c == current) Client_Focus(NULL, CurrentTime);
	Client_SetState(c, IconicState);
}

void
unhide(int n, int map) {
	Client *c;
	menuitem *prev = 0;
	menuitem *m = hidden_menu;

	/* Find the nth client. */
	if (n < 0)
		return;

	while (n-- > 0 && m != 0) {
		prev = m;
		m = m->next;
	}

	if (m == 0)
		return;

	c = m->client;

	/* Remove the item from the menu, and dispose of it. */
	if (prev == 0) {
		hidden_menu = m->next;
	} else {
		prev->next = m->next;
	}
	free(m);

	c->hidden = False;

	/* Unhide it. */
	if (map) {
		XMapWindow(dpy, c->parent);
		XMapWindow(dpy, c->window);
		Client_Raise(c);
		Client_SetState(c, NormalState);

		if (focus_mode == focus_click) {
			/* it feels right that the unhidden window gets focus*/
			Client_Focus(c, CurrentTime);
		}
	}
}

void
unhidec(Client *c, int map) {
	int i = 0;
	menuitem *m = hidden_menu;

	if (c == 0)
		return;

	/* My goodness, how the world sucks. */
	while (m != 0) {
		if (m->client == c) {
			unhide(i, map);
			return;
		}
		m = m->next;
		i++;
	}
}

void
menu_expose(void) {
	int i;		/* Menu item being drawn. */
	int width;	/* Width of each item. */
	int height;	/* Height of each item. */
	int length;	/* Number of menu items. */
	menuitem *m;

	getMenuDimensions(&width, &height, &length);

	/* Redraw the labels. */
	for (m = hidden_menu, i = 0; m != 0; m = m->next, i++) {
		int tx = (width - titleWidth(popup_font_set, m->client)) / 2;
		int ty = i * height + ascent(popup_font_set_ext);
		char *name;
		int namelen;

		if (m->client->menu_name == NULL) {
			name = m->client->name;
			namelen = m->client->namelen;
		} else {
			name = m->client->menu_name;
			namelen = m->client->menu_namelen;
		}

#ifdef X_HAVE_UTF8_STRING
		if (m->client->name_utf8 == True)
			Xutf8DrawString(dpy, m->client->screen->popup,
				popup_font_set, 
				current_screen->menu_gc, tx, ty,
				name, namelen);
		else
#endif
			XmbDrawString(dpy, m->client->screen->popup,
				popup_font_set,
				current_screen->menu_gc, tx, ty,
				name, namelen);
	}

	/* Highlight current item if there is one. */
	if (current_item >= 0 && current_item < length)
		XFillRectangle(dpy, current_screen->popup, current_screen->menu_gc,
			0, current_item * height, width, height);
}

void
menu_motionnotify(XEvent* ev) {
	int old;			/* Old menu position. */
	int width;		/* Width of menu. */
	int height;		/* Height of each menu item. */
	int length;		/* Number of menu items. */
	XButtonEvent *e = &ev->xbutton;
	
	getMenuDimensions(&width, &height, &length);
	
	old = current_item;
	current_item = menu_whichitem(e->x_root, e->y_root);
	
	if (current_item == old) return;
	
	/* Unhighlight the old position, if it was on the menu. */
	if (old >= 0 && old < length)
		XFillRectangle(dpy, current_screen->popup, current_screen->menu_gc,
			0, old * height, width, height);
	
	/* Highlight the new position, if it's on the menu. */
	if (current_item >= 0 && current_item < length)
		XFillRectangle(dpy, current_screen->popup, current_screen->menu_gc,
			0, current_item * height, width, height);
}

void
menu_buttonrelease(XEvent *ev) {
	int n;		/* Menu item. */

	/*
	 * Work out which menu item the button was released over.
	 */
	n = menu_whichitem(ev->xbutton.x_root, ev->xbutton.y_root);

	/* Hide the menu until it's needed again. */
	XUnmapWindow(dpy, current_screen->popup);/*BUG?*/

	/* Do the menu thing (of unhiding windows). */
	unhide(n, 1);

	if (current) {
		cmapfocus(current);
	}
}
