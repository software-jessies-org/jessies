<p>Evergreen started as a project to reimplement Rob Pike's <a href='http://en.wikipedia.org/wiki/Acme_(text_editor)'>Acme</a> editor in Java, for use on Unix and Windows instead of Plan 9. In the decade since then, it's evolved in directions that help it deal with large codebases, and multiple projects/branches at once. Remaining similarities include the tiled windows and the Unix-like reliance on external programs rather than reinventing every wheel. The major philosophical differences include strong support for keyboard-based editing, language-specific functionality, and native platform UI conventions.<br>
<br>
There are also two new guiding principles: accept regular expressions, output diffs.<br>
<br>
<p><img src='http://jessies.googlecode.com/svn/evergreen/trunk/www/main-window.png' alt="Evergreen's main window" width='822' height='594'>

<h2><a>Features</a></h2>

<ul><li><p><strong>Ubiquitous Regular Expressions</strong> - Programmers know regular expressions, and yet their graphical tools don't use make much use of them. Evergreen does. Anywhere it's asking you to type something, it's probably expecting a regular expression: full Perl/Java regular expressions in find, in find/replace, in find in files (as you'd expect), but also in the open dialog [<a href='http://jessies.googlecode.com/svn/evergreen/trunk/www/manual.html#regexps'>full details</a>].</li></ul>

<ul><li><p><strong>Open Files Quicker</strong> - Evergreen indexes your project, so you don't have to remember where files are. Or what case your co-worker used ("Hyperlink" or "HyperLink"?). Just type a substring or regular expression that matches what you want [<a href='http://jessies.googlecode.com/svn/evergreen/trunk/www/manual.html#opening-quickly'>full details</a>]. Evergreen updates a list of the matching files as you type (like iTunes):</li></ul>

<p align='center'><img src='http://jessies.googlecode.com/svn/evergreen/trunk/www/open-quickly.png' alt='Open Quickly dialog' width='591' height='322'>

<ul><li><p><strong>Find in Files</strong> - You can also search for files to open based on their content [<a href='http://jessies.googlecode.com/svn/evergreen/trunk/www/manual.html#opening-find'>full details</a>]. Evergreen shows a tree of matches, representing the directory hierarchy. Files containing definitions are marked as such. Searching is done in parallel to take advantage of modern multi-core machines. Search results are automatically updated if anything changes:</li></ul>

<p align='center'><img src='http://jessies.googlecode.com/svn/evergreen/trunk/www/find-in-files.png' alt='Find in Files dialog' width='723' height='502'>

<ul><li><p><strong>Find</strong> - You have a fast computer, yet the 'find' function in other editors doesn't take full advantage of that fact. Evergreen, like <tt>less(1)</tt>, highlights all the matches, whether you search with C-F or clicked on a match in the "Find in Files" dialog. Better still, Evergreen highlights all the matches as you type, so you know when you've typed enough (or too much, if you suddenly see there are no matches). Evergreen also lets you move backwards as easily as forwards through the matches: just use C-D to move backwards and C-G to move forwards, both conveniently placed around C-F, which defaults to searching for the currently-selected word. All of this comes together for fluid one-handed searching [<a href='http://jessies.googlecode.com/svn/evergreen/trunk/www/manual.html#editing-find'>full details</a>]. Finally, Evergreen uses marks next to the scroll bar to give you contextual information about how many matches there are, and how they're clustered. You can click on a mark to skip a large clump of uninteresting matches, if you wish:</li></ul>

<p align='center'><img src='http://jessies.googlecode.com/svn/evergreen/trunk/www/find.png' alt='Highlighted find matches' width='748' height='185'>

<ul><li><p><strong>Spelling Checking for Source Code</strong> - Not only does Evergreen check your spelling as you type, it understands CamelCase words aren't single-word spelling mistakes but compounds of correctly spelled words. In this example, the identifiers have been checked too:</li></ul>

<p align='center'><img src='http://jessies.googlecode.com/svn/evergreen/trunk/www/spelling.png' alt='Spelling checking of code' width='433' height='159'>

<ul><li><p><strong>Exuberant Ctags Support</strong> - Evergreen uses Exuberant Ctags (if installed) to understand the structure of your file [<a href='http://jessies.googlecode.com/svn/evergreen/trunk/www/manual.html#navigation-tags'>full details</a>]. The symbols in the current file are shown as a tree preserving their hierarchy. You can click on an item in the tree to go to the corresponding part of the file. Conversely, as you edit, the item in the tree corresponding to the current caret position is always highlighted.</li></ul>

<p align='center'>
<img src='http://jessies.googlecode.com/svn/evergreen/trunk/www/tags-panel.png' alt='Tags support' width='197' height='120'>

<ul><li><p><strong>Bug Database Links</strong> - References to bugs in your (or others') bug databases are automatically recognized and turned into hyperlinks:</li></ul>

<p align='center'>
<img src='http://jessies.googlecode.com/svn/evergreen/trunk/www/bug-links.png' alt='Bug database links' width='442' height='261'>

<ul><li><p><strong>Find/Replace</strong> - Find/replace functionality in other editors has a habit of being awkward in use. How much better to be able to see all the changes at once, with the results of the substitutions? Hovering over a match shows the captured groups in a tool tip, so you can check your capturing is as you intended [<a href='http://jessies.googlecode.com/svn/evergreen/trunk/www/manual.html#editing-replace'>full details</a>].</li></ul>

<p align='center'><img src='http://jessies.googlecode.com/svn/evergreen/trunk/www/find-replace.png' alt='Find and replace' width='706' height='530'>

<blockquote><p>(This is the obvious exception to the "output diffs" rule.)</blockquote>

<ul><li><p><strong>Workspaces</strong> - You can work on multiple projects at once, with each "workspace" getting its own tab in the overall UI [<a href='http://jessies.googlecode.com/svn/evergreen/trunk/www/manual.html#workspaces'>full details</a>].</li></ul>

<blockquote><p>Workspaces are important. You probably want one for each project you're working on; if you're looking for "project" or "session" functionality, this is it.</blockquote>

<blockquote><p>Each workspace's files will be indexed [<a href='http://jessies.googlecode.com/svn/evergreen/trunk/www/manual.html#workspaces-indexing'>full details</a>].</blockquote>

<blockquote><p>Your workspace configuration (including which files you're editing) is automatically saved when you quit and restored when you restart Evergreen.</blockquote>

<ul><li><p><strong>Auto-Indent</strong> - Evergreen will automatically help you format your code in K&R, Linux kernel, or Sun's Java style [<a href='http://jessies.googlecode.com/svn/evergreen/trunk/www/manual.html#editing-indentation'>full details</a>]. If you want to manually break long lines or use an indenting style too far removed from these, you're likely to face difficulties as it continues to try to "correct" your style. Ken Arnold's <a href='http://www.artima.com/weblogs/viewpost.jsp?thread=74230'>thoughts on coding style</a> explain why this is usually a counter-productive degree of freedom. What you gain from accepting Evergreen's style (which shouldn't be a problem for most programmers) is that Evergreen will do most of the formatting for you. Even correcting stuff you paste in.</li></ul>

<ul><li><p><strong>Watermarks</strong> - Each document has a watermark, used to show you if you're looking at a read-only file, or a file that has been updated on disk since it was last read in.</li></ul>

<ul><li><p><strong>Building</strong> - Evergreen defers to make(1) or ant(1) to actually build your project, and will search upward from the directory containing the focused file looking for a makefile [<a href='http://jessies.googlecode.com/svn/evergreen/trunk/www/manual.html#build'>full details</a>].</li></ul>

<ul><li><p><strong>Patches</strong> - If you choose a potentially destructive action such as "Revert to Saved", you're shown a colored patch and given a chance to change your mind. (Not only that, it's a useful way to see how a machine-generated file is changing as you work on the generator. Just keep running the script and reverting to the latest version of the generated file.)</li></ul>

<ul><li><p><strong>"Compare Selection and Clipboard..."</strong> - Shows you a patch comparing the currently-selected text and the text on the clipboard. This is useful when you're looking at two seemingly duplicate chunks of code and want to know what, if any, differences there are between the two.</li></ul>

<ul><li><p><strong>Language awareness</strong> - Coloring and intelligent indentation of C++, Java, Perl, Ruby, and others.</li></ul>

<ul><li><p><strong>Custom text component</strong> - We gave up on <pre><code>JTextPane</code></pre> because of its poor performance, and we gave up on <pre><code>JTextArea</code></pre> because of its poor functionality. Unlike many other editors with their own text components, ours was explicitly designed to be useful elsewhere. It's interface is broadly similar to <pre><code>JTextArea</code></pre> so it's easy to use in your own programs if you get used to any of the great functionality.</li></ul>

<h2><a>Documentation</a></h2>

<p>There's a separate <a href='https://code.google.com/p/jessies/source/browse/evergreen/trunk/www/manual.html'>user manual</a> for Evergreen.<br>
<br>
<h2><a>Mailing List</a></h2>

<p>Feel free to visit or join the <a href='http://groups.google.com/group/evergreen-users/'>evergreen-users</a> group.<br>
<br>
<br>
<h3>Building from Source</h3>
<p>See SalmaHayek.<br>
<br>
<h2>Authors</h2>

<p>The original implementation (which was much more like Wily) was by Elliott Hughes.<br>
<br>
<p>Ed Porter wrote a fast bitmap font renderer because in those days, Java's font rendering was too slow to be usable.<br>
<br>
<p>Phil Norman wrote the PTextArea text component we now use.<br>
<br>
<p>Sébastien Pierre contributed the icon.<br>
<br>
<p>Martin Dorey keeps us running on Windows.