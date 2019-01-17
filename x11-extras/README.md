# X11 Extras

These programs are all fairly minimalist, but that's the way I like them.

If you try any of these programs, please mail me to let me know how
you get on - whatever you think of them. I'm particularly interested in
generalisations or simplifications that further the programs' minimalist
nature.

To build:
```
cmake . && make
```

## clock - A Novel Clock

You might not notice that `clock` is running. It opens a tiny window (4
square pixels) in the top right of the screen and waits for the pointer
to enter it. When the pointer does enter, the window expands to display
the current date and time.

You can set what the "clock" actually displays using the viewCommand
resource. This is a program to be executed, whose output will be displayed
as the "time". You can thus call a shell-script which gives both the
time and battery percentage, say.

For left-handed users, there is the `leftHanded` resource. Giving this
a value will cause clock to open its window in the top _left_ corner of
the screen.

You can also set the action of the three mouse buttons using X
resources. I have xkill on button 2 and lock on button 3. So to lock the
screen, I throw the mouse and hit button 3. To kill a wayward program
I throw the mouse, hit button 2, find the window that's to die,
and kaboom!

## lock - X Display Locker

`lock` is a simple display locker so that you can safely leave your
X terminal unattended. It doesn't blank the screen (the X server can
do that - see xset(1)) and it doesn't have any pretty graphics. You're
supposed to be leaving your terminal unattended, remember? Buy yourself
a telly if you want something to watch.

Running `lock` locks all the screens on the display. To unlock the
display, type your password and press Return.

While the display is locked the screen is covered by a blank window and
the mouse cursor is hidden. A one-line message is displayed (by default
this is "This X display is locked. Please supply the password.").

As you type each successive character of your password, an asterisk is
shown. The Backspace key deletes the last character typed and the Esc
key starts again.

You can change both the one-line message and the font used to display it
using X resources. The message is resource `message` while the font is,
unsurprisingly, `font`. You can also change the system-wide defaults by
editing `lock.h` and recompiling.

On Linux systems, `lock` disables virtual consoles while running. 

Note that `lock` relies on passwords being stored in /etc/passwd, which
hasn't been true since the 1990s (when it was written). To be useful
today, it would need to be rewritten to use PAM.

## menu - X Menu

`menu` displays a menu at the top of the screen, based on a
specification in a `.menu` file which associates labels with commands
to be executed. There's also a clock at the very right-hand edge of the
menu bar. I started to use `menu` when a new server arrived at work,
and I could no longer fit everything I needed on the three buttons
offered by my earlier `clock` program.

This program is a bit rough around the edges, and has the misfortune of
working well enough that I have little inclination to polish it. If you
want to see improvement, you should prod me.  Those who don't live in
prodding distance could consider sending me mail.

## x11-reaper - Simple X Session Manager

```
Syntax: x11-reaper [daemon]
```

`x11-reaper` is an imitation of two programs that were part of Silicon
Graphics' IRIX operating system: reaper(1) and endsession(1).

The idea is to run `x11-reaper` as the last program in your `Xinitrc` or
`.xsession`, making it the program upon which the session hangs. It places
a property on the root window, and waits for it to be altered. When the
property is altered, the daemon `x11-reaper` terminates, causing the
X11 login session to end.

If run without an argument, `x11-reaper` behaves as endsession(1),
ending the login session. If run with an argument (say "daemon") then
it behaves as `reaper(1)`, waiting for the session to end.

In your `.xsession` (if you're a user installing this just for yourself)
or your `Xinitrc` (if you're an administrator installing this for
all users) ensure that the last line is something like `x11-reaper
daemon`. When you want to log out, run `x11-reaper` (from the command
line or from an on-screen menu or whatever). That's all there is to it!

[If you find these instructions too terse, then you probably shouldn't
be trying to follow them. It's advisable to know what you're doing,
or you may prevent yourself from being able to use your X server.]

## speckeysd - Special Keys Daemon

`speckeysd` is an imitation of Sun's Solaris program of the same name. It
lets you attach commands to hot-key combinations.

It's possible, for example, in combination with a program like `window`,
to add Alt-Tab or Alt-F4 style functionality to a window manager that
doesn't support hot keys. (`lwm` being an example that springs to mind.)

I only wrote this program to show that it could be done; I don't use
it myself. Because of this, the language for binding commands to keys
is rather primitive, but I'd be prepared to polish it up if there were
any demand for it.

Thanks to Tuncer Ayaz, Adam Sampson, and Anselm R. Garbe for their
improvements.

## window - X Window Controller

`window` lets you read and write some of the attributes of
X windows.

`wselect` shows a cursor and allows you to select a window. It then prints
that window's ID in hex.

The documentation gives examples of how to use these tools to do cool
stuff under X without having to do any X programming yourself.
