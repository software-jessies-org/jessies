<p>About the silly name: I needed a name for a repository containing useful Java classes shared by several projects.<br>
I couldn't think of a <i>good</i> name, so I went for a nice name instead.<br>
<br>
<h2><a>Features</a></h2>

<p>This list is very incomplete, but gives some idea of a few of the larger facilities on offer:<br>
<br>
<ul><li><p><strong>PTextArea</strong> - One of the bigger things we offer is a high-performance text area. It's single-font, like JTextArea, but it's multi-color, like JTextPane. It has none of JTextPane's performance problems, though, and it has a fairly simple interface for adding custom colorers for file types you care about. It comes with colorers for popular languages such as C++, Java, and Ruby.</li></ul>

<blockquote><p>Another big feature is built-in spelling checker support. It's even clever enough to check camelCaseWords if you're editing source, and also offers support for custom exception lists. It's fast, too, and unlike (say) Apple's built-in spelling checker is fast enough to check a whole document when you open it or paste, rather than waiting for you to move the caret out of a word before checking it.</blockquote>

<blockquote><p>There's also support for hyperlinks, both of the mailer kind where (say) <a href='http://software.jessies.org/'>http://software.jessies.org/</a> is automatically turned into a link, but also in more sophisticated applications where you want arbitrary recognized text to be turned into a link to some function of the recognized text. Edit and SCM use this to link to bug databases and RFCs, for example, so that simply saying "Sun 6227617" or "RFC 2229" in a comment (or in this document as I'm writing it) is enough to get you a link to the relevant document.</blockquote>

<ul><li><p><strong>Forms</strong> - Our forms package is also really useful. We think it's the best and easiest way to create dialogs. All the dialogs in all of our programs are produced this way, with a minimal amount of code. There's automatic support for all the features you'd expect, and it's even really easy to add "advanced" functionality like fields that search as-you-type.</li></ul>

<ul><li><p><strong>Build System</strong> - Disappointed by the amount of  boilerplate nonsense required by the likes of Ant, and disappointed by the relatively small amount of work such tools actually do for you, we've developed quite a sophisticated build system. It uses GNU make, but don't let that scare you: the whole point is that you can write pretty much anything (pure Java, Java + JNI, pure C++) and it'll do all the work for you.</li></ul>

<blockquote><p>If you want, we can also automatically generate installable distributions. For Mac users, we create Mac ".app" bundles (containing universal binaries for Intel and PowerPC) and compress them into ".dmg" files ready for distribution. For Linux users, we create ".deb" and ".rpm" packages. For Solaris users, we create ".pkg" packages. For MS&nbsp;Windows users, we create ".msi" installers.</blockquote>

<blockquote><p>If you look at the "Makefile" at the top of any of our projects, you'll see that none of them requires more than a single "include" line. That's it! (Strictly, if you want to generate Windows installers you need to specify a UUID for your project. But that's only one extra line, that by definition you should never change.)</blockquote>

<ul><li><p><strong>JavaHpp: <tt>javah(1)</tt> for C++ Programmers</strong> - Write your JNI code in C++ for improved clarity and correctness. See <a href='http://elliotth.blogspot.com/2005/06/better-jni-through-c.html'>Better JNI through C++</a> for the original motivation. In a nutshell, we generate a C++ class corresponding to the Java class, and automatically write C functions to call the appropriate member function on the C++ class. There's a member function for every native method, that you need to implement. There's a member variable for every field that proxies for the actual Java object's corresponding field, so you can read and write the field as if it were a C++ member variable. C++ exceptions thrown in your JNI code are translated to Java exceptions.</li></ul>

<blockquote><p>See the Terminator source for a good example of this in use.</blockquote>

<h2><a>Using our Debian package repository</a></h2>

<p>We used to have one of these but code.google.com doesn't support it.<br>
<br>
<h2><a>Downloads</a> / <a>Building from Source</a></h2>

<p>The <a href='https://code.google.com/p/jessies/source/list'>Changes</a> are available separately.<br>
<br>
<p>Anonymous read-only Subversion access is available. Copy and paste these commands to get everything you need:<br>
<br>
<pre>
mkdir ~/jessies && cd ~/jessies<br>
svn checkout http://jessies.googlecode.com/svn/salma-hayek/trunk/ salma-hayek<br>
make -C salma-hayek<br>
</pre>

Then you can optionally build our other main projects with:<br>
<br>
<pre>
svn checkout http://jessies.googlecode.com/svn/terminator/trunk/ terminator<br>
make -C terminator<br>
svn checkout http://jessies.googlecode.com/svn/terminator/trunk/ evergreen<br>
make -C evergreen<br>
</pre>

<p>There's no longer a source distribution, but Subversion makes it easier for you to keep up to date, and makes it easier to submit patches.<br>
<br>
<p>If you just want to look, you can <a href='https://code.google.com/p/jessies/source/browse/'>browse the repository</a>.<br>
<br>
<h3>All Platforms</h3>

<p>You need Java 6 or later.<br>
A JRE is enough to run our projects, but you need a JDK to build them.<br>
<br>
<p>You need Ruby 1.8 or later to run any of our projects.<br>
<br>
<p>You need GNU make 3.81 or later to build from source.<br>
The makefile will tell you what to do if you try to build with an old version.<br>
<br>
<p>To build the C++ parts, you'll need g++.<br>
<br>
<p>If you see this error, it means you're trying to build one of our projects without a copy of the salma-hayek library they all depend on:<br>
<pre>
Makefile:8: ../salma-hayek/lib/build/simple.make: No such file or directory<br>
make: *** No rule to make target `../salma-hayek/lib/build/simple.make'.  Stop.<br>
</pre>
<p>Check out or unpack a copy of salma-hayek and try again.<br>
<br>
<h3>Mac OS</h3>

<p>You need Mac OS 10.4 or later, and the latest Apple Java 6, available via Software Update.<br>
<br>
<p>You need the command-line tools supplied with Xcode 2.2.1 or later.<br>
The latest version of Xcode is available as a <a href='http://developer.apple.com/tools/download/'>free download from Apple</a>.<br>
Note that running the Xcode 2.2.1 installer will overwrite <tt>/usr/bin/make</tt> with version 3.80, so you need to upgrade to <a href='/3rdParty/make-3.81-darwin-universal'>GNU make 3.81</a> <i>after</i> upgrading Xcode.<br>
More recent versions of Xcode include an adequate make.<br>
<br>
<p>All our projects build universal binaries and libraries, suitable for both Intel and PowerPC Macs.<br>
<br>
<h3>Ubuntu</h3>

<p>We have more information about <a href='https://code.google.com/p/jessies/source/browse/salma-hayek/trunk/www/ubuntu-setup.html'>setting up Ubuntu for development</a>, including commands to install all the packages needed to build our projects.<br>
<br>
<h3>Windows</h3>

<p>Windows builds require Cygwin.<br>
We have more information about CygwinSetup if you're not already using it, and CygwinProblems.<br>
<br>
<p>To build the Windows installer, you need Microsoft's open source Windows installer compiler, <a href='http://sourceforge.net/projects/wix/'>WiX</a> version 2 or 3, installed and on your path.<br>
<br>
<p>If you see this error when building on Cygwin:<br>
<pre>
light.exe : error LGHT0001 : COM object with CLSID {F94985D5-29F9-4743-9805-99BC3F35B678} is either not valid or not registered.<br>
</pre>
<p>you also need MergeMod.dll to be installed and registered.<br>
An easy way to achieve this is to run the WiX version 3 installer.