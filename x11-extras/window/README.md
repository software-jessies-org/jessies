
NAME
	window, wselect - control over X windows

SYNOPSIS
	window -move <id> <x> <y>
	window -resize <id> <x> <y>
	window -raise <id>
	window -label <id> <text>
	window -lower <id>
	window -kill <id>
	window -hide <id>
	window -unhide <id>
	window -where <id>
	window -list
	window -getprop <id> <name>
	window -setprop <id> <name> <value>
	window -warppointer <id> <x> <y>
	window -getfocuswindow
	window -getsel
	window -delsel
	window -circup
	window -circdown
	wselect

DESCRIPTION
	Window lets you read and write some of the attributes of
	X windows. An <id> is a numeric window id (in one of C's
	bases), or the string "root" if you want the default root
	for your display.
	
	The -move and -resize options move and resize windows
	respectively.
	
	The -label option sets a window's title.
	
	The -raise and -lower options raise and lower windows
	respectively.
	
	The -kill option kills a window, like xkill.
	
	The -hide and -unhide options hide and unhide windows
	respectively.
	
	The -where option gives an X geometry string for a window.
	
	The -list option lists the current clients in tab-separated
	format: the first column gives the window id, the second
	the window's title.
	
	The -getprop option gets the value of the named property.
	
	The -setprop option sets the value of the named property.
	Only string properties are supported at the moment.
	
	The -warppointer option warps the pointer to the given
	offset in the given window.

	The -getfocuswindow option displays the id and name of
	the currently focused window.
	
	The -getsel option gets the current selection as a string.
	
	The -delsel option deselects the current primary selection.
	This is useful in combination with -setprop root CUT_BUFFER0
	if one wants to ensure that this will be used for pasting in xterm
	or whatever instead of the PRIMARY selection.
	
	The -circup and -circdown options circulate the windows
	up and down respectively.

	Wselect shows a cursor and allows you to select a window. The ID
	of the selected window is printed to standard output.

EXAMPLES
	Setting a shell window's title from the (rc) shell:
	
		fn prompt {window -label $WINDOWID `pwd}
	
	Implementing xkill(1) from the (rc) shell:
	
		fn xkill { window -kill `wselect}
	
	Displaying the display's resources:
	
		window -getprop root RESOURCE_MANAGER
	
	Opening a netscape window for the currently selected URL (the
	second half is for when netscape isn't currently running):
	
		netscape -noraise \
			-remote 'openBrowser('^`{window -getsel}^')' \
		|| netscape -no-about-splash `{window -getsel}
	
	Warping the pointer to 10, 10 in the current window (why?)
	using rc syntax:
	
		a=`{window -getfocuswindow} \
			window -warppointer $a(1) 10 10
	
AUTHOR
	Elliott Hughes <elliott.hughes@genedata.com>
	Stephen Parker <stephen@stephen.uk.eu.org>
