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
#ifdef SHAPE
#include <X11/extensions/shape.h>
#endif

#include "lwm.h"

/*ARGSUSED*/
extern void 
setShape(Client *c) {
#ifdef  SHAPE
	int n;
	int order;
	XRectangle *rect;

	if (shape) {
		XShapeSelectInput(dpy, c->window, ShapeNotifyMask);
		rect = XShapeGetRectangles(dpy, c->window, ShapeBounding,
			&n, &order);
		if (n > 1)
			XShapeCombineShape(dpy, c->parent, ShapeBounding,
				border - 1, border - 1, c->window,
				ShapeBounding, ShapeSet);
		XFree(rect);
	}
#else
#endif
}

/*ARGSUSED*/
extern int
shapeEvent(XEvent *ev) {
#ifdef  SHAPE
	if (shape && ev->type == shape_event) {
		Client *c;
		XShapeEvent *e = (XShapeEvent *)ev;

		c = Client_Get(e->window);
		if (c != 0)
			setShape(c);
		return 1;
	}
#else
#endif
	return 0;
}

/*ARGSUSED*/
extern int
isShaped(Window w) {
#ifdef SHAPE
	int	n;
	int	order;
	XRectangle	*rect;

	rect = XShapeGetRectangles(dpy, w, ShapeBounding, &n, &order);
	XFree(rect);

	return (n > 1);
#else
	return 0;
#endif
}

extern int
serverSupportsShapes(void) {
#ifdef SHAPE
	int shape_error;
	return XShapeQueryExtension(dpy, &shape_event, &shape_error);
#else
	return 0;
#endif
}
