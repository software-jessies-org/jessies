# Gummiband

Gummiband is a program launcher for the X Window System.
It can be configured to show machine status (eg battery %, or the time),
and to provide launch buttons (eg a button to run xterm).
It provides simple drop-down menus (eg a button can show the current wifi
access point, and clicking on the button opens a menu allowing a different
access point to be selected).

Gummiband is based loosely on the 'menu' program by Elliott Hughes.

![Gummiband in action](gummiband.png)

In the above screenshot, Gummiband is the white bar along the top of the screen,
while the Terminator terminal window is displaying the .gummiband configuration
file which produces the shown set of buttons. In the screenshot, I've clicked
on '[Rear Audio]', so the corresponding drop-down menu is showing which audio
outputs I can select between. Clicking on the 'front' item will run the program
`$HOME/bin/audio-socket front`.

See the Gummiband man page for more information on how to configure and use
the program.

```man ./gummiband.man```
