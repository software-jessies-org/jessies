<h2><a>This is <i>not</i> GNOME Terminator</a></h2>

<p>jessies.org's Terminator is not Chris Jones's <a href='http://www.tenshu.net/p/terminator/'>Terminator</a>, sometimes known as <a href='http://www.ohloh.net/p/gnome-terminator'>GNOME Terminator</a>.  Ours is a completely unrelated terminal emulator that was using the same lame name <a href='https://answers.launchpad.net/terminator/+question/25861'>first</a>.<br>
<br>
<p>Similarly, <tt>$HOME/.config/terminator/config</tt> is not our configuration file.  We don't support splitting windows.<br>
<br>
<h2>Why am I having trouble using the cursor keys, function keys or editing long lines?</h2>

<p>Most likely, either you don't have Terminator's terminfo data installed on the remote machine you're trying to use, or your TERM environment variable isn't set to "terminator".<br>
For detailed information on working out which is your problem, and how to fix either problem, see "Why am I having problems with terminfo/curses?" below.<br>
<br>
<h2>Where can I get a nice terminal font?</h2>
<p>Terminator will try to choose a good fixed-width/monospaced font for you, but it can only choose from what you have installed.<br>
<br>
<p>On Mac OS, you already have the traditional (for Apple) "Monaco" and alternatives of "Lucida Sans Typewriter" and, on Mac OS 10.6, the new "Menlo".<br>
<br>
<p>On Windows, if you have the JRE installed (rather than the JDK), then you may want to rerun the JRE installer and choose "Show advanced options panel" and then "Additional Font and Media Support" so that you get "Lucida Sans Typewriter" installed, otherwise you'll end up with "Courier New", and won't be able to distinguish the letter l from the number 1.<br>
<br>
<p>Many Windows users seem to like <a href='http://www.proggyfonts.com/'>proggy fonts</a>. You can also use these fonts on other operating systems, though in all cases you should download the TrueType version rather than the version specifically for your OS, because Terminator needs the TrueType version.<br>
<br>
<h2>Where can I get a nice terminal font for languages other than English?</h2>
<p>On Mac OS, see "Where can I get a nice terminal font?".<br>
Both of our recommendations cover Latin-based European languages, Chinese, Cyrillic, Greek, Japanese, Korean, and Thai.<br>
<br>
<p>On Linux, Solaris, and Windows, you'll need to install the operating system's support files for the language or languages you're interested in to be sure that you have sufficient font coverage. The safest choice is then to tell Terminator to use the logical "Monospaced" font, which is actually just a collection of native fonts that attempts to cover most of Unicode.<br>
<br>
<p>The exact fonts used by "Monospaced" for the different ranges varies between operating systems. One problem with "Monospaced" on Windows is that the ASCII range will be covered with "Courier New" (Linux and Solaris users will get "Lucida Sans Typewriter"). See <a href='http://java.sun.com/javase/6/docs/technotes/guides/intl/fontconfig.html'>Sun's fontconfig documentation</a> if you want to customize the choices.<br>
<br>
<p>(Note that Terminator currently doesn't cope well with far-east Asian languages, where glyphs tend to be wider than for European languages. Many terminal-based programs seem to have trouble too. If you'd like to help us improve Terminator's support for far-east Asian languages, please get in touch.)<br>
<br>
<h2>How can I set the window/tab title from the shell?</h2>
<p>Assuming you're using Bash, you need a function like this in your <tt>~/.bash_profile</tt>:<br>
<pre>
setWindowTitle() {<br>
echo -ne "\e]2;$*\a"<br>
}<br>
</pre>
<p>You can call this with any argument to set the window's title to that string. If the terminal running this is in a tab, the tab's title will be changed; a window with multiple tabs takes its title from the current tab.<br>
<br>
<p>This function will work for XTerm and most other terminal emulators too. One place it won't work is the Linux text console. For that reason, I recommend surrounding all the code in this FAQ section with something like this:<br>
<pre>
if [ "$TERM" == "terminator" ]<br>
then<br>
# window-title stuff here.<br>
fi<br>
</pre>

<p>If you have a prompt something like this:<br>
<pre>
export PS1="\[\033[33m\]\h:\w\$ \[\033[0m\]"<br>
</pre>
<p>You might want to achieve a similar effect in your title bar. Annoyingly, those \h and \w escapes (hostname without domain name and directory with $HOME rewritten as ~, respectively) only work in the PS1 environment variable. Luckily, Bash offers you an alternative way to achieve the same effect without the cost of running external programs (a common work-around):<br>
<pre>
updateWindowTitle() {<br>
setWindowTitle "${HOSTNAME%%.*}:${PWD/$HOME/~}"<br>
}<br>
PROMPT_COMMAND=updateWindowTitle<br>
</pre>
<p>Read the Bash manual page for full details of PS1, PROMPT_COMMAND, or the advanced forms of substitution.<br>
<br>
<h2>Why does my window/tab title keep changing in Cygwin?</h2>

<p>Cygwin's default <tt>/etc/profile</tt> includes an escape sequence in PS1 that sets the window title.<br>
Bearing in mind the answer to the previous question, if you want to disable this behavior on Cygwin, you will need to override its definition of PS1.<br>
<br>
<h2>How can I set the window/tab title from Vim?</h2>

<p>Add the line <tt>set title</tt> to your .vimrc and Vim will put the filename and some other information in the title bar.<br>
<br>
<h2>Why doesn't Bash always notice when the window changes size?</h2>

<p>If you don't care why this happens, just ensure that <tt>shopt | grep checkwinsize</tt> says that this option is on. If it's not, add <tt>shopt -s checkwinsize</tt> to your login script.<br>
<br>
<p>If you care why, keep reading...<br>
<br>
<p>Terminal emulators only indirectly tell their children when the window size changes. Really, they're just concerned with telling the kernel that the window size changed. This is done with an ioctl(2) system call. The kernel updates its information about the pseudo terminal and sends a SIGWINCH ("WINdow <a href='size.md'>size</a> CHange") signal to the process group that has the terminal. Applications can install a signal handler for this signal to update their own idea of the terminal size, and possibly redraw themselves.<br>
<br>
<p>In the olden days, this all worked fine. With the invention of shells with job control, though, shells started putting each pipeline in its own process group. (Note that from the shell grammar point of view, a pipeline may just be a simple command with no actual piping.) So the kernel sends the SIGWINCH to the foreground process group, as it always has, but now the shell is oblivious to this because it's in a different process group. Luckily, there's an ioctl(2) to read the window size, and if checkwinsize is set, Bash uses this to ask the kernel about the current window size whenever it regains control of the terminal.<br>
<br>
<h2>Why am I having problems with terminfo/curses?</h2>

<p>Terminator has its own terminfo file. This is regrettably necessary because of Terminator's unconventional approach to handling wide lines with a horizontal scrollbar rather than by wrapping them. The file will be installed under <tt>~/.terminfo</tt> the first time you run Terminator. If you used an installer that runs as root and can write to the system-wide terminfo directory (currently just the Linux installer), the terminfo file will be installed system-wide at install time.<br>
<br>
<p>If you run Terminator itself as root and the terminfo file has not been installed in the system-wide terminfo directory, it will be installed at that point.<br>
<br>
<p>If the terminfo file isn't available, the most common warnings you'll see are this one from programs such as <tt>less(1)</tt>:<br>
<pre>
WARNING: terminal is not fully functional<br>
-  (press RETURN)<br>
</pre>
<p>and this one, from <tt>vim(1)</tt>:<br>
<pre>
E558: Terminal entry not found in terminfo<br>
'terminator' not known. Available builtin terminals are:<br>
builtin_riscos<br>
builtin_amiga<br>
builtin_beos-ansi<br>
builtin_ansi<br>
builtin_pcansi<br>
builtin_win32<br>
builtin_vt320<br>
builtin_vt52<br>
builtin_xterm<br>
builtin_iris-ansi<br>
builtin_debug<br>
builtin_dumb<br>
defaulting to 'ansi'<br>
</pre>

<p>If you think you have the correct terminfo available but you're still having problems, the next thing to check is that you're actually <i>using</i> Terminator's terminfo. For example, if your problem is with your shell, check that <tt>echo $TERM</tt> says "terminator", and if your problem is with Vim, check that <tt>:se term</tt> says "term=terminator".<br>
<br>
<p>Although Terminator sets the TERM environment variable to "terminator" for you, your shell's profile may be setting it to an incorrect value. Also, if you're logging in to a remote machine, the value of TERM may not be correctly transferred. It's always worth checking as described in the previous paragraph. The correct fix is to change your profile so it doesn't override TERM, and to use protocols like SSH which transfer your terminal type. You definitely shouldn't force TERM to be "terminator" (or any other value) in your profile: that's how you get into this kind of trouble.<br>
<br>
<p>(See later for problems specific to Cygwin, Fedora/RedHat, and FreeBSD.)<br>
<br>
<h3>Terminfo problems logging in as other users/root</h3>
<p>If the terminfo file is not installed in the system-wide location then other users (and you yourself, if you use <tt>su(1)</tt> to switch to another user) will have problems. Using <tt>sudo(1)</tt> to run a command as another user will work in many cases, because it doesn't alter <tt>$HOME</tt> by default.<br>
<br>
<p>For best results, on computers where you'll be logged in as root at times, we recommend you install the terminfo file for Terminator in <tt>/usr/share/terminfo</tt>. The easiest way to do this is to simply run Terminator once as root. From then on, you can run Terminator as your normal user, and use <tt>su(1)</tt> to become root.<br>
<br>
If you find nothing in <tt>/usr/share/terminfo</tt> already, then perhaps your system uses a different root directory.<br>
<tt>man infocmp</tt> should tell you.<br>
<br>
<h3>Terminfo problems when using SSH</h3>
<p>You will also need to ensure that any machines you'll be remotely logging in to also have a copy of the terminfo file installed. If you use the same home directory on the local and remote machines, everything will just work. Likewise if someone has already run Terminator as root on the remote machine.<br>
<br>
<p>(Note that if you try to run Terminator on a remote machine, it probably won't get as far as opening a window. Nonetheless, it will get far enough to set up the terminfo file. Note also that running Terminator on a remote machine as a matter of course is usually a mistake, and will be <i>much</i> slower than running Terminator on your local machine and simply logging in to the remote machine from within Terminator using ssh(1) or whatever.)<br>
<br>
<p>If you're not able to run Terminator as root, you can simply run Terminator as yourself on the remote machine.<br>
<br>
<p>If you can't easily install or run Terminator on the remote machine, you only need to copy the terminfo file to <tt>~/.terminfo/t/terminator</tt> (you'll need to <tt>mkdir -p ~/.terminfo/t</tt> if the directories don't already exist). Note that if your remote machine is a Mac, you'll need to copy the file to <tt>~/.terminfo/74/terminator</tt> because of a long-standing Mac OS bug (0x74 is the byte representing 't' in ASCII); running Terminator automatically covers both cases, for completeness.<br>
<br>
<p>On Linux or Mac OS, and most other ncurses-based systems, this is all you need to do. If you still have problems like the ones shown at the start of this section, add the following lines to your profile on the remote machine:<br>
<pre>
export TERMINFO=~/.terminfo<br>
export TERM=$TERM<br>
</pre>

<h3>Terminfo problems specific to Cygwin</h3>
<p>Cygwin-1.5's less(1) still uses a termcap-based ncurses library.<br>
If less(1)&nbsp;&ndash; and hence man(1)&nbsp;&ndash; say "WARNING: terminal is not fully functional" and upgrading to Cygwin-1.7 is not an option, then try:<br>
<pre>
infocmp -Cr >> ~/.termcap<br>
</pre>

<p>infocmp is provided by Cygwin's ncurses package.<br>
<br>
<h3>Terminfo problems specific to Fedora/RedHat/CentOS</h3>
<p>RedHat installs vim-minimal to provide /bin/vi.<br>
vim-minimal avoids the use of terminfo, seemingly deliberately, if frustratingly.<br>
We don't know of a simple, complete way of persuading RedHat to always use one of its other vim packages, which use terminfo.<br>
(The sudo package, for example, depends explicitly on vim-minimal.)<br>
<br>
<p>If you need to use applications, like vim-minimal, which are compiled with termcap, then first install Terminator on the RedHat box or otherwise install the Terminator terminfo there.  Then:<br>
<pre>
infocmp -Cr >> ~/.termcap<br>
</pre>

<p>Even with termcap set up, so that eg cursors work, we don't know how to make vim-minimal produce colors with TERM=terminator.<br>
We suggest that you install vim-enhanced, which colors out of the box, anyway and use that (via <tt>vim</tt> rather than <tt>vi</tt>) when you have a choice.<br>
If you know how to beat vim-minimal into coloring with TERM=terminator, then we'd love to know.<br>
<br>
<p>Finally, it seems necessary to add this line, or an equivalent, to your .bash_profile or equivalent:<br>
<br>
<pre>
export TERMCAP=~/.termcap<br>
</pre>

<h3>Terminfo problems specific to FreeBSD</h3>
<p>FreeBSD systems don't come with the necessary terminfo support installed by<br>
default.  It's apparently a snap to install the<br>
<a href='http://www.freebsd.org/cgi/url.cgi?ports/devel/ncurses/pkg-descr'>devel/ncurses</a>
package from the <a href='http://www.freebsd.org/ports/'>FreeBSD Ports and Packages Collection</a>.<br>
<br>
<h3>Terminfo problems specific to AIX</h3>
<p>We believe <tt>/usr/share/lib/terminfo</tt> to be the appropriate directory.<br>
When the terminfo file is missing, telnetd sets <tt>$TERM</tt> to <tt>TERMINATOR</tt> (when it should be <tt>terminator</tt>).<br>
<br>
<h2>Why am I having trouble with Emacs?</h2>

<p>By default, Terminator uses the alt key for its keyboard equivalents for menu actions (except on Mac OS). If you want to use the alt key as Emacs' meta, you need to tell Terminator so in the preferences dialog. Then Terminator will use control+shift for its keyboard equivalents, and alt-key combinations will be passed through to the application.<br>
<br>
<h2>Why am I having trouble with Emacs on Mac OS?</h2>

<p>Mac Emacs users can't use the alt key as Emacs' meta because bugs in Apple's Java VM mean that Terminator isn't given the correct key presses. In some cases (alt-e, alt-u, alt-i, and alt-n) we're not given any key press at all. We've raised Apple bug ID#5158674, and will have to wait for Apple to fix their JVM. Emacs works fine in Terminator on Linux, Solaris, and Windows, if that's an alternative for you.<br>
<br>
<h2>How do I change my default shell?</h2>

<p>Terminator will always start the shell specified by the SHELL environment variable.<br>
(It will fall back to /bin/sh if the SHELL environment variable isn't set.)<br>
You could simply influence Terminator's environment, but the best solution is to tell your operating system which shell you'd like, and it will ensure that SHELL is set correctly for all applications, not just Terminator.<br>
See the documentation for your operating system for details on how to change your default shell.<br>
<br>
<h2>How can I open a new terminal from the command line?</h2>

<p>You can start other programs, even in multiple tabs, from the command line.<br>
See <tt>terminator --help</tt> for the syntax.<br>
Here's an example that opens a new window with two tabs:<br>
<pre>
terminator -n "top's tab's title" top -n "another tab title" bash<br>
</pre>

<h2>How can I open a new terminal from Nautilus?</h2>

<p>If you install the <tt>nautilus-open-terminal</tt> package and restart Nautilus<br>
(with eg killall nautilus, though surely there must be a better way),<br>
Nautilus will gain an "Open Terminal" menu item.<br>
In the system-wide "Preferred Applications" preference dialog, you can choose "Custom" as your terminal emulator.<br>
Enter "terminator" as the command, and "-e" as the execute flag.<br>
<br>
<p>If you have GNOME 2.21 or later, "Terminator" will already be an available option.<br>
Immortal honor, in the ChangeLog, awaits the first person to submit a patch such that Terminator's icon appears in that list, like those of the other alternatives.<br>
<br>
<h2>Why can't I paste in VNC?</h2>

<p>If you're using an old version of VNC, you may experience problems pasting into Terminator, with the following exception in the "Debugging Messages":<br>
<pre>
java.awt.datatransfer.UnsupportedFlavorException: Unicode String<br>
</pre>
<p>This is caused by a bug in old versions of the X11 VNC server. On Debian, you should remove the <tt>vncserver</tt> package and install the <tt>vnc4server</tt> package, and run <tt>vnc4server</tt> (it takes the same arguments as <tt>vncserver</tt>)&nbsp;&mdash; just upgrading your existing <tt>vncserver</tt> package will not help. Switching package will also a fix a bunch of other problems you may or may not have noticed with VNC.<br>
<br>
<h2>Why can't I paste into rdesktop?</h2>

<p>We recommend that you run rdesktop with the -r clipboard:CLIPBOARD switch.<br>
Without the switch, Terminator's X11 selection can't be pasted into rdesktop for reasons unknown.<br>
In any case, as Windows has no direct equivalent of pasting the selection, the symmetrical approach seems likely to cause less confusion.<br>
<br>
<h2>Why does Terminator sometimes flash/flicker the whole window?</h2>

<p>A lot of terminal-based programs (such as Bash and Vim) attempt to ring the terminal's bell to indicate that you're trying to do something impossible.<br>
Terminator implements an alternative, called "visual bell".<br>
The default rendering of the visual bell is to briefly flash the entire window a gentle shade of your normal background color.<br>
<br>
<p>If you find that the effect is too slow, try turning off the preference "High-quality rendering of the visual bell".<br>
This uses a cheaper (XOR) effect that doesn't look as nice, but which is more efficient for slow graphics hardware or network use such as VNC.<br>
<br>
<p>If you really just don't want any effect, you can turn off the preference "Visual bell (as opposed to no bell)", in which case Terminator will do nothing when an application tries to ring the bell.<br>
<br>
<h2>Can Terminator access serial ports?</h2>

<p>On Windows, terminal emulators are often combined in the same program with telnet/SSH clients, or code that accesses serial ports.<br>
The Unix tradition is that terminal emulators don't care <i>what</i> they're running, will run whatever you ask them to, and don't have any other programs welded on.<br>
Terminator follows the Unix tradition (including on Cygwin).<br>
<br>
<p>There are various choices for accessing serial ports from Terminator.<br>
The simplest choice is probably socat(1) (<a href='http://www.dest-unreach.org/socat/'><a href='http://www.dest-unreach.org/socat/'>http://www.dest-unreach.org/socat/</a></a>).<br>
The ability to stick GNU readline between you and the serial port is pretty nice, if you think how awful most serial devices' consoles are.<br>
If you're an old-school Unix minimalist type, this is probably what you're looking for.<br>
<br>
<p>Many people on the net recommend "minicom" (<a href='http://alioth.debian.org/projects/minicom/'><a href='http://alioth.debian.org/projects/minicom/'>http://alioth.debian.org/projects/minicom/</a></a>).<br>
If you want a hierarchical text-based menu interface, like DOS programs of the 1980s, this is the one for you.<br>
This might be a better initial choice if you're unfamiliar with serial ports or otherwise need to experiment to find the right settings.<br>
<br>
<p>We're told that screen(1) (<a href='http://www.gnu.org/software/screen/'><a href='http://www.gnu.org/software/screen/'>http://www.gnu.org/software/screen/</a></a>) works too: try <tt>sudo screen /dev/ttyS0</tt>.<br>
If you already use screen(1), this may be just what you're looking for.<br>
If you don't, you might find the learning curve very steep (because screen has a lot of functionality).<br>
<br>
<p>Serial concentrators turn serial connections into telnet ones, rendering the issue moot.<br>
I've had bad experiences with old models being both slow and dropping characters from one port when another port is under load.<br>
"Digi PortServer 16", though, I can recommend as being worth the several hundred dollar price tag.<br>
<br>
<p>If you're actually looking for XMODEM, YMODEM, ZMODEM file transfer, lrzsz(1) (<a href='http://www.ohse.de/uwe/software/lrzsz.html'><a href='http://www.ohse.de/uwe/software/lrzsz.html'>http://www.ohse.de/uwe/software/lrzsz.html</a></a>) is excellent.<br>
Its UI could be improved; it would be nice if the frame-by-frame progress reports were as concise and useful as HyperTerminal's.<br>
(If you did this, engineers working on new hardware bringup want to know if packets are being dropped, how long the transfer's going to take and whether the transfer rate is what it should be. Graphing transfers and errors against time would give them more useful information than they get from HyperTerminal, without drowning them as lrzsz(1) currently does.)<br>
<br>
<h2><a>How do I turn the logging off?</a></h2>

<p>Why do you want to?<br>
In our experience, a software developer generates about 1 GiB of logs per year.<br>
On a 1 TiB disk, you won't notice the missing 0.5% by the time the spinning rust seizes.<br>
We've been through several disks like this.<br>
The numbers of changed since before our beards were grey.<br>
<br>
<p>Oh, you're in QA?<br>
Try this in your crontab:<br>
<br>
<pre><code><br>
@daily find ~/.terminator/logs -mtime +30 | xargs --no-run-if-empty rm<br>
</code></pre>

<p>If that still doesn't do it for you, then:<br>
<br>
<pre><code><br>
chmod -w ~/.terminator/logs<br>
</code></pre>

<p>Logging and scrollback are independent.<br>
<br>
<h2>I'm having problems with Cygwin!</h2>

<p>We have a page devoted to CygwinSetup and another about CygwinProblems, which may help.