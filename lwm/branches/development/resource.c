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

#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>

#include <X11/X.h>
#include <X11/Xos.h>
#include <X11/Xlib.h>
#include <X11/Xresource.h>
#include <X11/Xutil.h>
#include <X11/Xatom.h>

#include "lwm.h"

char	*font_name;		/* User's choice of titlebar font. */
char	*popup_font_name;	/* User's choice of menu font. */
char	*btn1_command;		/* User's choice of button 1 command. */
char	*btn2_command;		/* User's choice of button 2 command. */
int	border;			/* User's choice of border size. */
FocusMode focus_mode;		/* User's choice of focus mode (default enter) */

char *
sdup(char *p) {
	char	*s ;

	s = malloc(strlen(p) + 1);
	if(s == 0)
		panic("malloc failed.");
	return strcpy(s, p);
}

extern void
parseResources(void) {
	XrmDatabase	db;
	XrmValue	value;
	char	*resource_manager;
	char	*type;

	/* Set our fall-back defaults. */
	font_name = DEFAULT_TITLE_FONT;
	popup_font_name = DEFAULT_POPUP_FONT;
	border = DEFAULT_BORDER;
	btn1_command = 0;
	btn2_command = DEFAULT_TERMINAL;
	focus_mode = focus_enter;

	resource_manager = XResourceManagerString(dpy);
	if (resource_manager == 0)
		return;

	XrmInitialize();
	db = XrmGetStringDatabase(resource_manager);
	if (db == 0)
		return;

	/* Fonts. */
	if (XrmGetResource(db, "lwm.titleFont", "Font", &type, &value) == True)
		if (strcmp(type, "String") == 0)
			font_name = sdup((char *) value.addr);
	if (XrmGetResource(db, "lwm.popupFont", "Font", &type, &value) == True)
		if (strcmp(type, "String") == 0)
			popup_font_name = sdup((char *) value.addr);

	/* Window border width. */
	if(XrmGetResource(db, "lwm.border", "Border", &type, &value) == True)
		if (strcmp(type, "String") == 0)
			border = (int) strtol((char *) value.addr, (char **) 0, 0);

	/* The button commands. */
	if (XrmGetResource(db, "lwm.button1", "Command", &type, &value) == True)
		if (strcmp(type, "String") == 0)
			btn1_command = sdup((char *) value.addr);
	if (XrmGetResource(db, "lwm.button2", "Command", &type, &value) == True)
		if (strcmp(type, "String") == 0)
			btn2_command = sdup((char *) value.addr);

	/* The focus mode */
	if (XrmGetResource(db, "lwm.focus", "FocusMode", &type, &value)
		== True && strcmp(type, "String") == 0) {
		if (strcmp(value.addr, "enter") == 0) focus_mode = focus_enter;
		if (strcmp(value.addr, "click") == 0) focus_mode = focus_click;
	}
}
