<h1>Cygwin Setup</h1>

<p></p>

<p>(See also <a href='CygwinProblems.md'>CygwinProblems</a>.)<br>
<br>
<h2>What is Cygwin?</h2>
<p>Cygwin is a Linux-like environment for MS Windows. You can read more about it on the <a href='http://www.cygwin.com'>Cygwin home page</a>, but this page assumes you know what it is and have already decided that you want it. If you're a Unix user and you're stuck with a Windows box, you probably do. It's quite a remarkable hack.<br>
<br>
<h2>Installation</h2>
<p>Download the <a href='http://cygwin.com/setup-x86.exe'>32 bit</a> or <a href='http://cygwin.com/setup-x86_64.exe'>64 bit</a> Cygwin setup.exe (and keep it for later, because you'll want it to update your installation too). Run it now, and hit Next. Answer "Install from Internet" and hit Next. Answer "c:\cygwin", "All Users", "Unix / binary", and hit Next. I don't think the local package directory matters, so just hit Next. Hit Next again. Choose a nearby server and hit Next.<br>
<br>
<h3>Basic packages</h3>
<p>You'll now be asked to select packages. The interface is a nightmare along the lines of a mouse-driven dselect(1). Most people seem to have trouble guessing how to use it, so read the next few paragraphs before you start clicking.<br>
<br>
<p>For the initial installation, don't select any (extra) packages, just hit Next. When the main part of the installation completes, allow the installer to create a desktop shortcut.<br>
<br>
<p>Cygwin is now installed.<br>
<br>
<p>In future, assuming you only want to update the packages already on your system, rather than adding new ones, you can simply start the installer and keep hitting "Next" without any other interaction. It remembers what you've already told it.<br>
Now setup.exe has been primed with your preferences, you can update your installation using "unattended" mode with -q.<br>
<br>
<p>You might want to make use of that right away to install extra packages.<br>
We suggest:<br>
<br>
<pre>
inetutils  # telnet(1)<br>
ncurses    # see Terminator FAQ<br>
openssh    # SSH (client and server, though the server's not so great)<br>
perl<br>
python<br>
ruby       # run Perl, Python, or Ruby scripts<br>
vim        # edit stuff locally<br>
</pre>

<p>Cygwin 1.7's setup.exe introduces a -P switch which takes a list of extra packages.<br>
Setup needs to be run from a cmd window, not from bash.<br>
Drag the shortcut to setup.exe from your Desktop into the cmd window.<br>
<br>
<pre>
...\setup.exe -q -P inetutils,ncurses,openssh,perl,python,ruby,vim<br>
</pre>

<p>In the unlikely event that you want to build our projects, you also need:<br>
<br>
<pre>
gcc-g++,make,mingw-runtime,subversion<br>
</pre>

<p>To use Evergreen in its full glory, you also want:<br>
<br>
<pre>
aspell-en,ctags<br>
</pre>

<p>Other programs useful in developing these projects have included:<br>
<br>
<pre>
cron,exim,patch<br>
</pre>

<p>To install all the packages recommended on this page and others that have been found useful when working on these projects, then:<br>
<br>
<pre>
...\setup.exe -q -P aspell-en,cron,ctags,exim,gcc-g++,inetutils,make,mingw-runtime,ncurses,openssh,patch,perl,python,ruby,rxvt,subversion,vim<br>
</pre>

<p>Should you ever have to do battle with Cygwin-1.5's package browser, click the "View" button until the label next to it says "Full". This flattens out the list of packages and makes it easier to peruse.<br>
<br>
<p>Cygwin-1.7's version has an edit control in the top left, the contents of which are searched for in package names.<br>
A substring match puts the package onto the shortlist, which is then much more manageable.<br>
<br>
<p>Click once on each of the weird circular icons on the lines with packages you want. The weird circular icons cycle between various of Skip, available version numbers, Keep, Uninstall, Reinstall and Source, depending on what's available and already installed. You want the first version number offered. If you accidentally click past it, keep clicking until it cycles round again.<br>
<br>
<h2>Configuration</h2>

<p>Double-click on the Cygwin icon on your desktop.<br>
With Cygwin-1.5, you'll get a message like this:<br>
<pre>
Your group is currently "mkgroup".  This indicates that<br>
the /etc/group (and possibly /etc/passwd) files should be rebuilt.<br>
See the man pages for mkpasswd and mkgroup then, for example, run<br>
mkpasswd -l [-d] > /etc/passwd<br>
mkgroup  -l [-d] > /etc/group<br>
Note that the -d switch is necessary for domain users.<br>
</pre>
<p>If you don't know otherwise then you should follow the suggestion, including the note about -d where relevant.  After doing that, double-click on the Cygwin icon <i>again</i> to set up your home directory.  If you don't then, for example, Terminator will fail to find your home directory and refuse to start.<br>
<br>
<p>Even on Cygwin-1.7, you will want to follow the above instructions if, for example, ssh is to think of your home directory as something other than /tmp.<br>
<br>
<h3>Home directory</h3>

<p>If you have a CIFS share on your real Unix box (via Samba, say), it is tempting to map that as a network drive (U:, say). You can then set the Windows environment variable HOME to "U:\" and Cygwin will do the right thing, including running your existing login scripts. (You may want to have installed Perl and Ruby first, and you'll want to follow the usual rules when messing around with login scripts. In particular, keep a working window open at all times!)<br>
<br>
<p>You can set Windows environment variables from the control panel. Double-click "System", click the "Advanced" tab, and click the "Environment Variables..." button. Click the upper "New..." button to create a new user variable.<br>
<br>
<p>While you're there, add a variable called LANG, set to en_US.UTF-8.<br>
<br>
<p>Cygwin's interoperability with Samba has sometimes been problematic.<br>
Using a local directory as your home also tends to be faster.<br>
One way of doing this is to set HOME, instead, to C:\Documents and Settings\martind.<br>
The previously suggested mkpasswd incantation will have set your home directory in /etc/passwd to /home/martind,<br>
so mkdir /home/martind within Cygwin and add an entry to Cygwin's /etc/fstab:<br>
<br>
<pre>
echo 'C:/Documents\040and\040Settings/martind /home/martind ntfs binary 0 0' >> /etc/fstab<br>
</pre>

<p>After editing /etc/fstab, quit all of your Cygwin processes to activate the changes.<br>
By doing this immediately, rather than using the mount command, you will be ensured that everything will work after a reboot.<br>
<br>
<p>In the suggestion above, when Cygwin creates a Cygwin equivalent for the Windows environment variable HOME, it translates the Windows path into the longest Cygwin mount that matches, hence /home/martind.<br>
Unlike the Windows original, this doesn't contain spaces.<br>
Unix directory names rarely contain spaces and makefiles, in particular, usually don't cope.<br>
The mount won't circumvent the issue where the makefiles invoke native Windows tools, like the Java compiler or the tools used to build Windows installers.<br>
<br>
<p>A local home directory whose name includes no space will thus be the least problematic, if also the least convenient, choice.<br>
Whatever you choose to set HOME to, the entry in /etc/passwd should match.<br>
<br>
<h3>Default permissions</h3>
<p>As mentioned in <a href='http://cygwin.com/ml/cygwin/2006-01/msg00257.html'><a href='http://cygwin.com/ml/cygwin/2006-01/msg00257.html'>http://cygwin.com/ml/cygwin/2006-01/msg00257.html</a></a>, the default permissions for a Cygwin installation are, or at least used to be, rwxrwxrwx. Even if you don't care about the security implications, Ruby does. You might get sick of seeing Ruby make complaints like:<br>
<pre>
warning: Insecure world writable dir /usr/local/bin, mode 040777<br>
</pre>
<p>The directory and exact mode may differ, but you'll need to <tt>chmod o-w</tt> the directory in question. Here's the minimal set of changes I needed to make to keep Ruby quiet:<br>
<pre>
chmod o-w /usr/local/bin<br>
chmod o-w /usr/local<br>
chmod o-w /usr<br>
chmod o-w /etc<br>
chmod o-w /usr/sbin<br>
chmod o-w /usr/bin<br>
chmod o-w /usr/X11R6/bin<br>
chmod o-w /usr/X11R6<br>
chmod o-w /cygdrive/c<br>
</pre>
<p>Of course, this still leaves every binary on your system world-writable. Strangely, Ruby's happy to run world-writable binaries, as long as the directory they're in isn't world-writable.<br>
<br>
<h2>Hints and Tips</h2>

<h3>Cygwin's setup log</h3>
<p>If you update your Cygwin installation frequently, sometimes things will break or just change in a way you're not expecting, and you'll find yourself wondering what version you were running on a particular day. Matt Wilkie offers this tip to see the dates on which you installed the various versions you've used:<br>
<pre>
grep -e 'Installing .*cygwin-[0-9].*\.bz2' /var/log/setup.log<br>
</pre>
<p>The log contains details of all the packages you've installed, in case you're interested in something other than the base.