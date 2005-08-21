package terminator.view.highlight;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.regex.*;
import javax.swing.*;
import e.util.*;

import terminator.*;
import terminator.model.*;
import terminator.view.*;

/**
 * Tries to create links to files.
 */
public class HyperlinkHighlighter implements Highlighter {
	/** The underlined blue standard hyperlink style. */
	private final Style style = new Style(Options.getSharedInstance().getColor("linkColor"), null, null, Boolean.TRUE, false);
	
	private String directory = "~/";
	
	/*
	 * The regular expression constructed below deliberately matches too
	 * much. We look more closely at its matches later on.
	 * 
	 * We build the expression up in pieces, because it's
	 * complicated, and difficult to understand. Note the uses of ?> to
	 * prevent backtracking. The regular expression this replaced used to
	 * contain three points where backtracking could occur, and the output
	 * of something like
	 * 
	 *   perl -e 'print "x" x 100_000 . "\n"'
	 * 
	 * could really bring Terminator to a complete stop for four whole
	 * minutes. We're now down to ~40ms on the same machine, which is
	 * a decimal order of magnitude worse than Perl, suggesting a missing
	 * optimization.
	 * 
	 * Thanks to Markus Laker for this optimized regular expression.
	 */
	
	/**
	 * A filename can start at the beginning of a line, after a quote, or
	 * after whitespace.
	 */
	private static final String START = "(?: ^ | \" | \\s )";
	
	/**
	 * A filename can end at the end of a line, before a quote, or before
	 * whitespace. It can also reasonably end with any character, after
	 * the address part of a grep match. So there's not much to say here.
	 */
	private static final String END = "";
	
	/**
	 * The (optional) directory part of a filename must end in a slash,
	 * and may not contain whitespace, quotes, or colons.
	 */
	private static final String DIRECTORY = "(?> [^\\s:\"] * / ) ?";
	
	/**
	 * The mandatory leaf part of a filename can either be "Makefile", or
	 * non-whitespace before a '.' followed by non-whitespace. But there
	 * has to be a '.', or we assume it's uninteresting.
	 * 
	 * The first character class subtraction (a Java extension) lets us
	 * match names such as "sigwinch-test.cpp" while keeping our
	 * non-backtracking match of names containing a '.'.
	 * 
	 * The second character class subtraction stops us matching run-on
	 * text in a grep match like "file.cpp:123:#if 0".
	 */
	private static final String NAME = "(?: Makefile \\b | (?> [\\S && [^\\.]]+ ) \\. [\\S && [^:]]+ )";
	
	/**
	 * We're actually looking for a grep-style address, where there's an
	 * optional :line:column:line:column sequence after the filename itself.
	 * These are the forms Edit recognises although the pattern isn't quite
	 * the same as the copy in Edit.java.
	 */
	private static final String ADDRESS = "((:\\d+){1,4})?";
	
	/** The complete pattern. */
	private static final Pattern PATTERN =
		Pattern.compile("(?x)" +
	                        START +
	                        "(" + DIRECTORY + NAME + ADDRESS + ")" +
	                        END);
	
	/**
	 * This is the group within 'pattern' that should be taken as a filename.
	 */
	private int relevantGroup = 1;
	
	/**
	 * Puts off work until we've not been asked to do anything for a while.
	 * The code that calls us to addHighlights does so rather eagerly, and
	 * that can really hurt when we're looking at a very long line.
	 */
	private class WorkDeferrer implements ActionListener {
		private Timer timer;
		private JTextBuffer view;
		private int firstLineIndex;
		
		public WorkDeferrer() {
			this.timer = new Timer(100, this);
			this.timer.setRepeats(false);
			this.firstLineIndex = Integer.MAX_VALUE;
		}
		
		public void defer(JTextBuffer view, int lineIndex) {
			this.view = view;
			this.firstLineIndex = Math.min(firstLineIndex, lineIndex);
			timer.restart();
		}
		
		public void actionPerformed(ActionEvent e) {
			reallyAddHighlights(view, firstLineIndex);
			this.view = null;
			this.firstLineIndex = Integer.MAX_VALUE;
		}
	}
	
	private WorkDeferrer workDeferrer = new WorkDeferrer();
	
	public String getName() {
		return "Hyperlink Highlighter";
	}
	
	/**
	 * Lets us be notified of title changes; we assume that the title is the name of the current directory.
	 */
	public void setDirectory(String name) {
		this.directory = name;
	}
	
	/** Request to add highlights to all lines of the view from the index given onwards. */
	public int addHighlights(JTextBuffer view, int firstLineIndex) {
		workDeferrer.defer(view, firstLineIndex);
		return 0;
	}
	
	/**
	 * Invoked by the WorkDeferrer when it's time to do some work.
	 */
	private void reallyAddHighlights(JTextBuffer view, int firstLineIndex) {
		TextBuffer model = view.getModel();
		for (int i = firstLineIndex; i < model.getLineCount(); i++) {
			String line = model.getTextLine(i).getString();
			addHighlightsOnLine(view, i, line);
		}
	}
	
	private int addHighlightsOnLine(JTextBuffer view, int lineIndex, String text) {
		if (text.length() == 0) {
			return 0;
		}
		
		int count = 0;
		//long startTime = System.currentTimeMillis();
		Matcher matcher = PATTERN.matcher(text);
		while (matcher.find()) {
			/*
			 * We're most useful in providing links to grep matches, so we
			 * need to avoid being confused by stuff like File.java:123.
			 */
			String name = matcher.group(relevantGroup);
			int colonIndex = name.indexOf(':');
			if (colonIndex != -1) {
				name = name.substring(0, colonIndex);
			}
			
			/*
			 * If the file doesn't exist, this wasn't a useful match.
			 * This implementation is problematic, though, because
			 * we risk hanging the AWT event thread. Maybe a better
			 * trade-off is to have a list of acceptable extensions
			 * and only accept those, but always accept them?
			 */
			File file = null;
			if (name.startsWith("/") || name.startsWith("~")) {
				file = FileUtilities.fileFromString(name);
				// We can't be certain that an NFS server will
				// ever respond, so using File.exists is a bit
				// dangerous. For now, use a closed world
				// assumption.
				continue;
			} else {
				file = FileUtilities.fileFromParentAndString(directory, name);
				if (file.exists() == false) {
					continue;
				}
			}
			
			Location start = new Location(lineIndex, matcher.start(relevantGroup));
			Location end = new Location(lineIndex, matcher.end(relevantGroup));
			Highlight highlight = new Highlight(HyperlinkHighlighter.this, start, end, style);
			highlight.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			view.addHighlight(highlight);
			++count;
		}
		//System.err.println("addHighlightsOnLine (text.length() = " + text.length() + ") took " + (System.currentTimeMillis() - startTime) + " ms");
		return count;
	}

	/** Request to do something when the user clicks on a Highlight generated by this Highlighter. */
	public void highlightClicked(final JTextBuffer view, Highlight highlight, String text, MouseEvent event) {
		Matcher matcher = PATTERN.matcher(text);
		while (matcher.find()) {
			final String command = getEditor() + " " + matcher.group(relevantGroup);
			ProcessUtilities.ProcessListener listener = new ProcessUtilities.ProcessListener() {
				public void processExited(int status) {
					if (status != 0) {
						Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, view);
						frame.toFront();
						JOptionPane.showMessageDialog(frame, "Process '" + command + "' exited with status " + status + ".", "Terminator", JOptionPane.WARNING_MESSAGE);
					}
				}
			};
			ProcessUtilities.spawn(FileUtilities.fileFromString(directory), new String[] { "bash", "--login", "-c", command }, listener);
		}
	}
	
	private static String getEditor() {
		String result = System.getenv("EDITOR");
		if (result == null) {
			result = "edit";
		}
		return result;
	}
}
