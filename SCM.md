<p>SCM is currently unavailable. This is to ensure our compliance with new restrictions in the BitKeeper 4 license. SCM will not return until six months after the last of us stops using BitKeeper.<br>
<br>
<p>We're sorry for the inconvenience. In the meantime, our guide to writing good check-in comments remains in its original form below...<br>
<br>
<h2>Writing check-in comments</h2>

<p>When you start CheckInTool, it will tell you what to do next.<br>
The idea is that you check a file you want to commit, then type the comment corresponding to that file, then repeat for all the other files you want to include in the change set.<br>
<br>
<p>It turns out to often be convenient to group files.<br>
Rather than repeating a comment, just group the affected files together.<br>
The GNU style allows you to repeat a filename if you have more than one thing to say about it, but SCM doesn't really help you with that: you'll have to copy and paste the name yourself.<br>
Another common practice is to show related sub-changes and their relation by writing ellipses between chunks of the story.<br>
<br>
<p>Here's an example of the kind of comment SCM will naturally help you write:<br>
<blockquote><pre>
src/e/scm/WaitCursor.java: making this a class full of static data and methods<br>
was stupid. To cope with nested tasks, change it to something you instantiate...<br>
<br>
src/e/scm/BlockingWorker.java:<br>
src/e/scm/PatchView.java:<br>
src/e/scm/RevisionWindow.java: ...and change all the callers.<br>
</pre></blockquote>
<p>You can write anything you like, of course, but you'll get less help.<br>
<br>
<p>The text area is 80 columns wide, but the text is only soft-wrapped.<br>
If you want line breaks, you'll have to type them yourself.<br>
Anywhere SCM shows you a comment, it will word-wrap for you, so hard line breaks shouldn't be necessary.<br>
<br>
<p>BitKeeper supports giving a different comment to each file in a change set, and yet another comment to the set as a whole.<br>
SCM doesn't support this.<br>
One reason is that other back ends don't.<br>
But more than that, experience suggests that this freedom isn't useful, and does cause problems.<br>
One common case is that people accidentally hide important details by pasting the same comment for most of the files, but editing one or more of the copies.<br>
This is very easily missed when you're reading the history.<br>
Another common case is where important detail is hidden in the change set comment, which isn't visible when you're looking at one file's history.<br>
In all cases, it's unnecessarily awkward to see the commentary on the change set as a whole.<br>
You could argue that this is a weakness of the tools, but I think it's a weakness of the idea itself.<br>
The dialog style of commenting works better, and fits the idea of a change set as a coherent group of changes.<br>
<br>
<p>In effect, SCM forces you to write only a change set comment, which it then applies to each file, and to the change set.<br>
<br>
<h2><a>Writing <i>good</i> check-in comments</a></h2>

<p>One reason check-in comments are useful is, as Chris Aston always says, it's a safe assumption that "I'm not cleverer today than I was yesterday".<br>
So if you see something that's wrong, or weird, it's worth trying to find out where it came from.<br>
And that's when you're dealing with your own code! It's even more important to do a bit of archaeology if you're working on someone else's code.<br>
<br>
<p>This helps guide us in writing good check-in comments.<br>
As you write, imagine someone who's left the company has already written the comment, and you're only reading it.<br>
Think about what you'd most like to see.<br>
<br>
<p>I explained above why you should comment the "change set", naming all the files touched, and the relationship between the individual files' changes.<br>
<br>
<p>Here's a checklist of things you should try to get into a habit of going through in your mind as you write a check-in comment:<br>
<br>
<ul><li><b>Bug/defect number</b> - if you have a bug database, reference it. There's likely to be plenty of extra information/dialog in there that shouldn't go to waste. This is why SCM and Edit go out of their way to turn your bug/defect numbers into links.</li></ul>

<ul><li><p><b>Other people involved</b> - if you go under a bus on your way home from work, who else would be worth talking to when it comes to this code? Especially useful if you're doing something as the result of a request without a formal defect number, or as the result of group consensus.</li></ul>

<ul><li><p><b>Alternatives rejected</b> - we can see what you <i>did</i> implement, but it's often interesting to know why you didn't implement other alternatives. Often this is interesting enough to warrant code comments instead. Especially useful if you're replacing one implementation with another is why the old implementation was no longer suitable.</li></ul>

<ul><li><p><b>Known limitations</b> - these should probably be FIXME or TODO comments in the code.</li></ul>

<ul><li><p><b>Known performance impact</b> - if you measured the performance impact of your change, say so. Mention the tests and the results.</li></ul>

<ul><li><p><b>Evidence to support "premature" optimization</b> - if you're introducing an optimization, point to a demonstration that it's useful, or details of the situations in which it's useful. This will discourage others from backing it out rather than fixing it when it's demonstrated to be incorrect.</li></ul>

<ul><li><p><b>Text of build errors fixed</b> - if you're fixing a build error, include the exact error text to help later reviewers determine whether it's the most appropriate fix or understand your problem so that they don't re-introduce the build error.</li></ul>

<p>I won't insult you by reminding you that saying <i>what</i> you've done is never as useful as <i>why</i> you did it, though such comments can be useful if you accidentally add/change/remove code you didn't mean to touch.<br>
If it's not mentioned in the check-in comment, that lends credence to the belief that it was a mistake.<br>
<br>
<p>I also won't insult you by reminding you that duplication is bad in comments as well as code.<br>
Don't repeat yourself (except in mentioning bug/defect numbers, which should always appear in the check-in comment, and should often appear in the source too), and favor commenting the source.<br>
The main reason for commenting a check-in is that it's often the only way to express the commonality between changes made to different files.<br>
This is another reason to favor the "change set" style of commenting: someone looking at any file can trivially see the context of the change.<br>
<br>
<p>One final tip: the more likely it is that someone will need a piece of information to understand the code, the stronger the pressure to include that information in a code comment rather than a check-in comment.