package terminator;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;

/**
 * Implements the feel (rather than the look) of the selection. The look is
 * deferred to the Highlighter. The feel is the usual sweep-selection, plus
 * double-click to highlight a word (the exact definition of which is biased
 * towards shell-like applications), triple-click for line selection, and
 * shift-click to extend a selection.
 */
public class Selector implements MouseListener, MouseMotionListener, Highlighter {
	private JTextBuffer view;
	private Highlight highlight;
	private Location startLocation;
	
	private static final StyleMutator STYLER;
	static {
		Color selectionColor = Options.getSharedInstance().getColor("selectionColor");
		if (selectionColor == null) {
			STYLER = new SelectedStyleMutator();
		} else {
			STYLER = new Style(Options.getSharedInstance().getColor("foreground"), selectionColor, null, null);
		}
	}
	
	/** Creates a Selector for selecting text in the given view, and adds us as mouse listeners to that view. */
	public Selector(JTextBuffer view) {
		this.view = view;
		view.addMouseListener(this);
		view.addMouseMotionListener(this);
		view.addHighlighter(this);
		view.setAutoscrolls(true);
	}
	
	public void mousePressed(MouseEvent event) {
		if (event.getButton() != MouseEvent.BUTTON1) {
			return;
		}
		
		// Shift-click should move one end of the selection.
		if (event.isShiftDown() && startLocation != null) {
			selectToPointOf(event);
			return;
		}
		
		Location loc = view.viewToModel(event.getPoint());
		view.removeHighlightsFrom(this, 0);
		startLocation = loc;
		
		if (loc.getLineIndex() >= view.getModel().getLineCount()) {
			return;
		}
		
		if (event.getClickCount() == 2) {
			selectWord(loc);
		} else if (event.getClickCount() == 3) {
			selectLine(loc);
		}
	}
	
	public void mouseClicked(MouseEvent event) {
		if (event.getButton() == MouseEvent.BUTTON2) {
			view.paste();
		}
	}
	
	/**
	 * Copies the selected text to the clipboard.
	 */
	public void copy() {
		if (highlight != null) {
			setClipboard(view.getTabbedText(highlight));
		}
	}
	
	public boolean isWordChar(char ch) {
		// Space marks the end of a word by any reasonable definition.
		// Bracket characters usually mark the end of what you're interested in.
		// Likewise quote characters.
		return " <>(){}[]`'\"".indexOf(ch) == -1;
	}
	
	public void selectWord(Location location) {
		final int lineNumber= location.getLineIndex();
		String line = view.getModel().getLine(lineNumber);
		if (location.getCharOffset() >= line.length()) {
			return;
		}
		
		int start = location.getCharOffset();
		int end = start;
		while (start > 0 && isWordChar(line.charAt(start - 1))) {
			--start;
		}
		while (end < line.length() && isWordChar(line.charAt(end))) {
			++end;
		}
		startLocation = new Location(lineNumber, start);
		setHighlight(startLocation, new Location(lineNumber, end));
	}
	
	public void selectLine(Location location) {
		Location start = new Location(location.getLineIndex(), 0);
		Location end = new Location(location.getLineIndex() + 1, 0);
		startLocation = start;
		setHighlight(start, end);
	}
	
	public void mouseReleased(MouseEvent event) {
		if (event.getButton() == MouseEvent.BUTTON1) {
			copy();
		}
	}
	
	/**
	 * Sets the clipboard (and X11's nasty hacky semi-duplicate).
	 */
	public static void setClipboard(String newContents) {
		StringSelection selection = new StringSelection(newContents);
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		toolkit.getSystemClipboard().setContents(selection, selection);
		if (toolkit.getSystemSelection() != null) {
			toolkit.getSystemSelection().setContents(selection, selection);
		}
	}
	
	public void mouseDragged(MouseEvent event) {
		if (startLocation != null) {
			selectToPointOf(event);
		}
	}
	
	/**
	 * Changes the end of the selection to be the point pointed to by the
	 * given MouseEvent.
	 */
	private void selectToPointOf(MouseEvent event) {
		view.removeHighlightsFrom(this, 0);
		Location endLocation = view.viewToModel(event.getPoint());
		if (endLocation.equals(startLocation)) {
			return;
		}
		setHighlight(min(startLocation, endLocation), max(startLocation, endLocation));
		view.scrollRectToVisible(new Rectangle(event.getX(), event.getY(), 10, 10));
	}
	
	public void mouseMoved(MouseEvent event) { }
	
	public void mouseEntered(MouseEvent event) { }
	
	public void mouseExited(MouseEvent event) { }
	
	private void setHighlight(Location start, Location end) {
		TextLine startLine = view.getModel().get(start.getLineIndex());
		start = new Location(start.getLineIndex(), startLine.getEffectiveCharStartOffset(start.getCharOffset()));
		// Cope with selections off the bottom of the screen.
		if (end.getCharOffset() != 0) {
			TextLine endLine = view.getModel().get(end.getLineIndex());
			end = new Location(end.getLineIndex(), endLine.getEffectiveCharEndOffset(end.getCharOffset()));
		}
		highlight = new Highlight(this, start, end, STYLER);
		view.addHighlight(highlight);
	}

	// Highlighter methods.
	
	public String getName() {
		return "Selection Highlighter";
	}
	
	/** Request to add highlights to all lines of the view from the index given onwards. */
	public void addHighlights(JTextBuffer view, int firstLineIndex) {
		if (highlight != null && isValidLocation(view, highlight.getStart()) && isValidLocation(view, highlight.getEnd())) {
			view.addHighlight(highlight);
		} else {
			highlight = null;
		}
	}
	
	private boolean isValidLocation(JTextBuffer view, Location location) {
		TextBuffer model = view.getModel();
		if (location.getLineIndex() >= model.getLineCount()) {
			return false;
		}
		TextLine line = model.get(location.getLineIndex());
		if (location.getCharOffset() >= line.length()) {
			return false;
		}
		return true;
	}

	/** Request to do something when the user clicks on a Highlight generated by this Highlighter. */
	public void highlightClicked(JTextBuffer view, Highlight highlight, String highlitText, MouseEvent event) { }

	public static class SelectedStyleMutator implements StyleMutator {
		public Style mutate(Style style) {
			// Not obvious when you first look at it, but this just switches foreground and background colours.
			return new Style(style.getBackground(), style.getForeground(), style.isBold(), style.isUnderlined());
		}
	}
	
	// These two should be in java.lang.Math, but they're not.
	
	public Location min(Location one, Location two) {
		return (one.compareTo(two) < 0) ? one : two;
	}
	
	public Location max(Location one, Location two) {
		return (one.compareTo(two) > 0) ? one : two;
	}
}
