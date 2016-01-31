<h1>Cygwin Problems</h1>

<p></p>

<p>(See also <a href='CygwinSetup.md'>CygwinSetup</a>.)<br>
<br>
<h2>bash immediately and continually crashes on starting Terminator, or spins after a cd</h2>

<p>Terminator had problems on many systems with Cygwin releases after 1.7.7.  The mailing list contains evidence that these persisted up to and including 1.7.15 but were remedied by the time of 1.7.17.  Whatever changed to fix it was in Cygwin, rather than Terminator, and we never did work out what that was.  That wasn't the first such breakage and perhaps it won't be the last.  When it breaks again, and you want to try a downgrade, instructions for 1.7.7 were <a href='http://groups.google.com/group/terminator-users/msg/d2c57e9306c3d6ae'>noted</a> on the mailing list.<br>
<br>
<h2>Double-clicking on the shortcut icon does nothing</h2>

<p>If you're having trouble starting one of our programs, please try running it from a Cygwin Bash prompt<br>
instead of from the desktop shortcut icon until you work out what's wrong.<br>
We have had several of these problems in the past.<br>
<br>
<p>Please mail us a bug report including the output when you start the program from a Cygwin Bash prompt.<br>
(The web page for the particular project you're trying to run should tell you the email address.)<br>
<br>
<p>Any console output from our applications (as opposed to the start-up scripts or their interpreters) is recorded in a file.<br>
For Terminator, these are stored in <tt>~/.terminator/logs/</tt> along with a file containing a log of each tab's output.<br>
Evergreen's output is logged in your temporary directory.<br>
SCM's output is not currently logged.<br>
<br>
<h2>Why do we use Cygwin?</h2>

<p>The main reason for using Cygwin is that one of our projects, Terminator, requires it.<br>
NT's built-in POSIX subsystem won't let you launch native Windows applications or interact with the network, so that wasn't an option.<br>
Cygwin, on the other hand, even contains the Unix98 pseudo-terminal support which we use to support terminal resizing, not to mention containing a port of one of the most widely used ssh programs.<br>
<br>
<p>Cygwin is also convenient for us in that it lets us use the same start-up scripts that we use on Linux and Mac OS.<br>
<br>
<p>In future, the other projects may be runnable without Cygwin.  Even then, if you're programming or doing system administration on Windows, installing and learning Cygwin would probably be an excellent investment of your time.<br>
<br>
<p>We don't distribute the Cygwin DLL because that would cause <a href='http://www.cygwin.com/faq/faq.using.html#faq.using.multiple-copies'>problems</a> for our users and because it would <a href='http://www.cygwin.com/license.html'>require</a> us to distribute the Cygwin source (which would cost storage and bandwidth and administration effort).<br>
<br>
<h2>Why does ls(1) not show me Chinese characters?</h2>

<p>All our projects are designed to work with UTF-8.<br>
As of Cygwin-1.7, Cygwin supports UTF-8 filenames.<br>
You may need to configure Cygwin applications, like ls, to produce UTF-8 output and to interpret input as UTF-8.<br>
I did this on XP with Control Panel, System, Advanced, Environment Variables, System and make a LANG variable with the value en_US.UTF-8.<br>
<br>
<h2>Why can't java-launcher find jvm.dll on 64 bit Windows?</h2>

<p>All our projects are designed to work with both 64 bit and 32 bit processors.<br>
Terminator needs to use a Cygwin-compiled Java launcher in order to be able to load a JNI DLL that uses Cygwin's pseudo-terminal support.<br>
Terminator, Java and Cygwin must all have the same bit width.<br>
The 32 bit versions of Terminator, Java and Cygwin all work fine on 64 bit Windows.<br>
As of fall 2013, though, there are 64 bit Windows versions of all three.<br>
<br>
<h2>Why does telnet(1) quit without doing anything?</h2>

<p>Microsoft's telnet program does that when started from Cygwin terminals like Terminator and rxvt.<br>
It works fine from the pre-2011 "Cygwin icon", which launched a console subsystem window, but not from real terminals, which are Windows subsystem applications.<br>
We recommend that you install Cygwin's inetutils package, which installs a fully working /usr/bin/telnet.<br>
<br>
<p>Microsoft's ftp program exhibits a similar symptom, producing no output.<br>
The solution is, once again, to install Cygwin inetutils.<br>
<br>
<p>Similar behavior has been reported for at least one other console subsystem application.