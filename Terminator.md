Terminator will run on any modern OS with Java 6 or later.

It replaces xterm, rxvt, xwsh and friends on X11 systems, GNOME Terminal, KDE's Konsole, Apple's Terminal.app, and PuTTY on Windows.

### Features ###

Here are some of the features unique to Terminator, or which are rare amongst the competition:

  * **Automatic Logging** - Complete logs are automatically generated of all your terminal sessions.

  * **Drag & Drop** - Text and URLs, and even files from Finder/Nautilus/Windows Explorer can be dropped on Terminator to be inserted as text, with automatic quoting of filenames containing shell meta-characters.

  * **Find** - Terminator provides you with a find function so you can search for text and regular expressions within your terminal (including the scrollback), highlighting them all, in the style of `less(1)`, and offering quick movement to the next or previous match.

> As far as we know, the only other terminal emulator with a find function is Apple's Terminal, and ours is better.

> Searches stay active until you cancel them, so if you're waiting for some particular output in a great stream of output, this is a really great way to make it blindingly obvious when it appears.

> A screenshot of Terminator on Mac OS, after running `man bash`, and doing a find for `(?i)shell`:

> Despite the evidence that a pager is running, this is Terminator doing the highlighting --- one of the authors uses `cat(1)` as his pager.

  * **Freedom** - Terminator is released under the GPL.

  * **Horizontal Scrolling** - Most terminal emulators wrap text when it intrudes upon the right margin. Terminator doesn't --- it instead provides a horizontal scrollbar when necessary (hold down shift to make your scroll wheel scroll horizontally). This brings clear benefits in terms of readability of program output, or text files sent to the display with `cat(1)`. (We tried both this and Apple-like reflowing of text when you change the window size, and we decided we preferred this. Sadly, we don't have the resources to support two implementations, so reflowing is no longer an option; it's gone from the code.)

  * **Multiple Tabs** - Like tabbed browsing, only with terminals.

  * **Number Reinterpretation** - Terminator will recognize numbers in a variety of bases as the current selection, and add informational menu items to the pop-up menu showing the same number in other bases. No more `man ascii` or resorting to `bc(1)`.

  * **Open Terminator Here** - Start a new terminal window in the directory you clicked on in Windows Explorer.

  * **Portability** - Written mostly in Java, with a small POSIX C++ part (for pseudo-terminal support) and a Ruby invocation script, Terminator should compile out of the box on most modern desktop operating systems. (Pre-built packages are available for most systems, so you don't need to worry about this.)

> Now you can use the same great terminal emulator everywhere!

  * **Proper Tab Character Handling** - Most terminal emulators will translate tab characters into strings of spaces, which is very annoying if you then try to copy/paste a section of text from your terminal into a text editor. Terminator handles tabs properly, remembering where the tabs are and copy/pasting them as tab characters.

  * **Unlimited Scrollback** - Terminator won't throw away output when it scrolls off the top of the screen, nor when it reaches any arbitrary limit. _You_ decide when, if ever, to clear the scrollback. (Even if you do clear the scrollback, you can still find lost text in the log, unless you turned that off too.)

  * **Intelligent Vertical Scrolling** - Terminator's scrollbar won't keep jumping when there's output if you've deliberately scrolled back to look at part of the history, but as soon as you scroll back to the bottom again, it will resume auto-scrolling. This gives you the best of both worlds without having to choose up-front via some configuration mechanism.

  * **Fewer Gotchas** - Terminator automatically turns off XON/XOFF flow control for terminal output. Being able to pause output by typing <sup>S and resume it by typing </sup>Q is much less useful given unlimited scrollback and intelligent vertical scrolling, and mainly leads to confusion when it's accidentally typed by someone who doesn't know about this functionality (or doesn't realize they've activated it). Turning off XON/XOFF allows Bash and Emacs, amongst others, to make use of these keystrokes.

> A user who wants the XON/XOFF behavior -- perhaps because they like to pause the output from commands like `top(1)` -- can use `stty ixon` in their login script to re-enable it. Sadly, we can't just visibly indicate when ^S has stopped output, and provide help in re-starting output, because there's no way to read this aspect of tty state.

  * **Safe Quit** - Terminator knows when you still have processes running, and brings up a dialog rather than just letting those processes die.

  * **UTF-8** - Terminator won't mangle your favorite accented characters, and it copes well with languages such as Greek where there's a mix of normal and wide glyphs:
<p align='center'><img src='http://jessies.googlecode.com/svn/terminator/trunk/www/mac-os-greek.png' alt='Greek on Mac OS'>
<blockquote>The text is Markus Kuhn's <a href='http://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-demo.txt'>UTF-8-demo.txt</a> if you want to see what a mess Terminal.app makes of it. (On Mac OS, Terminal.app beats us on Thai, but we're better at Cyrillic, Greek, and Japanese. Results vary between platforms based on availability and suitability of fonts.)</blockquote></li></ul>

> One side-effect of proper support for variable-width glyphs is that you're not restricted to fixed-width fonts in Terminator, even if the only language you're using is English. GNU readline might get confused about the line length, though.

> Here's a more convincingly terminal-like example of fixed-width English and variable-width Japanese; output from javac(1):
<p align='center'><img src='http://jessies.googlecode.com/svn/terminator/trunk/www/javac-jp-monaco.png' alt='Japanese javac(1) output on Mac OS'></li></ul>


We think Terminator is the clear choice for the discerning terminal user.
Though originally written for Linux to make up for the fact that no Linux terminal emulator was as good as Mac OS' Terminal, it has now surpassed Terminal in several areas.

## Mailing List ##

Feel free to visit or join the <a href='http://groups.google.com/group/terminator-users/'>terminator-users</a> group.

## Authors ##

The original implementation and documentation was written by Phil Norman.

The original idea of having a suitably hackable terminal emulator with which to experiment with advanced features came from Elliott Hughes, and he turned Phil's component into an application.

Martin Dorey was responsible for the Cygwin port; we wrote more about <a href='http://elliotth.blogspot.com/2005/08/porting-jni-code-to-win32-with-cygwin.html'>Porting JNI code to Win32 with Cygwin</a> and [java.io.FileDescriptor on Win32](http://elliotth.blogspot.com/2005/08/javaiofiledescriptor-on-win32.html).  With the advent of Cygwin64, we now support Win64 natively.

Phil wrote the code that took us from nothing to a more-or-less usable replacement for `rxvt` on his own between 2004-04-21 and 2004-05-28. Given that he was on holiday for a week during this time, that made roughly a month of development time.

Since then, Terminator has been on the long slog towards Joel's <a href='http://www.joelonsoftware.com/articles/fog0000000017.html'>ten years</a> of quality.

Sébastien Pierre contributed the icon.

Matt Hillsdon added URL highlighting and made Terminator easier to embed in Eclipse.

Costantino Cerbo added the close icons to tabs.

Ben Longbons rewrote the style attributes code, adding support for 256-color mode and konsole-compatible 24-bit color.

Simon Sadedin added support for block selection when dragging with the Ctrl key.