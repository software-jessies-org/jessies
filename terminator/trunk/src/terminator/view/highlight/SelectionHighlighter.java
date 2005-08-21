package terminator.view.highlight;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import javax.swing.*;

import terminator.*;
import terminator.model.*;
import terminator.view.*;

/**
 * Implements the feel (rather than the look) of the selection. The look is
 * deferred to the Highlighter. The feel is the usual sweep-selection, plus
 * double-click to highlight a word (the exact definition of which is biased
 * towards shell-like applications), triple-click for line selection, and
 * shift-click to extend a selection.
 */
public class SelectionHighlighter implements Highlighter, ClipboardOwner, MouseListener, MouseMotionListener {
	/**
	 * Subverts the immutability of Style so we can track focus changes
	 * and effectively update the selection highlight's background color
	 * in response.
	 */
	private class SelectionStyle extends Style implements FocusListener {
		private final Color unfocusedSelectionColor = new Color(0.5f, 0.5f, 0.5f);
		private boolean focused = true;
		
		private SelectionStyle() {
			super(Options.getSharedInstance().getColor("foreground"), Options.getSharedInstance().getColor("selectionColor"), null, null, false);
		}
		
		public Color getBackground() {
			if (focused) {
				return super.getBackground();
			}
			return unfocusedSelectionColor;
		}
		
		public void focusGained(FocusEvent e) {
			focused = true;
			view.repaint();
		}
		
		public void focusLost(FocusEvent e) {
			focused = false;
			view.repaint();
		}
	}
	
	private SelectionStyle style = new SelectionStyle();
	
	private JTextBuffer view;
	private Highlight highlight;
	private Location startLocation;
	
	/** Creates a SelectionHighlighter for selecting text in the given view, and adds us as mouse listeners to that view. */
	public SelectionHighlighter(JTextBuffer view) {
		this.view = view;
		view.addFocusListener(style);
		view.addMouseListener(this);
		view.addMouseMotionListener(this);
		view.addHighlighter(this);
		view.setAutoscrolls(true);
	}
	
	public void mousePressed(MouseEvent event) {
		if (SwingUtilities.isLeftMouseButton(event) == false || event.isPopupTrigger()) {
			return;
		}
		
		// Shift-click should move one end of the selection.
		if (event.isShiftDown() && startLocation != null) {
			selectToPointOf(event);
			return;
		}
		
		Location loc = view.viewToModel(event.getPoint());
		view.removeHighlightsFrom(this, 0);
		highlight = null;
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
			view.middleButtonPaste();
		}
	}
	
	public void mouseReleased(MouseEvent event) {
		if (SwingUtilities.isLeftMouseButton(event)) {
			selectionChanged();
		}
	}
	
	public void mouseDragged(MouseEvent event) {
		if (SwingUtilities.isLeftMouseButton(event) && startLocation != null) {
			selectToPointOf(event);
		}
	}
	
	public void mouseMoved(MouseEvent event) {
	}
	
	public void mouseEntered(MouseEvent event) {
	}
	
	public void mouseExited(MouseEvent event) {
	}
	
	public boolean isWordChar(char ch) {
		// Space marks the end of a word by any reasonable definition.
		// Bracket characters usually mark the end of what you're interested in.
		// Likewise quote characters.
		return " <>(){}[]`'\"".indexOf(ch) == -1;
	}
	
	public void selectWord(Location location) {
		final int lineNumber= location.getLineIndex();
		String line = view.getModel().getTextLine(lineNumber).getString();
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
		selectionChanged();
	}
	
	private void clearSelection() {
		view.removeHighlightsFrom(this, 0);
		startLocation = null;
		highlight = null;
	}
	
	public void selectAll() {
		Location start = new Location(0, 0);
		Location end = new Location(view.getModel().getLineCount(), 0);
		startLocation = start;
		setHighlight(start, end);
		selectionChanged();
	}
	
	private void selectLine(Location location) {
		Location start = new Location(location.getLineIndex(), 0);
		Location end = new Location(location.getLineIndex() + 1, 0);
		startLocation = start;
		setHighlight(start, end);
		selectionChanged();
	}
	
	/**
	 * Copies the selected text to the clipboard.
	 */
	public void copyToSystemClipboard() {
		copyToClipboard(view.getToolkit().getSystemClipboard());
	}
	
	/**
	 * Copies the selected text to X11's selection.
	 * Does nothing on other platforms.
	 */
	public void copyToSystemSelection() {
		Clipboard systemSelection = view.getToolkit().getSystemSelection();
		if (systemSelection != null) {
			copyToClipboard(systemSelection);
		}
	}
	
	/**
	 * Copies the selected text to X11's selection (behaving like X terminals)
	 * and Windows's clipboard (behaving like PuTTY).
	 * Does nothing on Mac OS X (meaning that the menu showing "info"
	 * about the selection presumably doesn't work there)..
	 */
	private void selectionChanged() {
		if (e.util.GuiUtilities.isWindows()) {
			copyToSystemClipboard();
		} else {
			copyToSystemSelection();
		}
	}
	
	private void copyToClipboard(Clipboard clipboard) {
		if (highlight == null) {
			return;
		}
		String newContents = getTabbedString();
		StringSelection selection = new StringSelection(newContents);
		clipboard.setContents(selection, this);
	}
	
	public String getTabbedString() {
		return (highlight != null) ? view.getTabbedString(highlight) : "";
	}
	
	/**
	 * Invoked to notify us that we no longer own the clipboard; we use
	 * this to clear the selection, so we're not misrepresenting the
	 * situation.
	 */
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		clearSelection();
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
	
	private void setHighlight(Location start, Location end) {
		TextLine startLine = view.getModel().getTextLine(start.getLineIndex());
		start = new Location(start.getLineIndex(), startLine.getEffectiveCharStartOffset(start.getCharOffset()));
		// Cope with selections off the bottom of the screen.
		if (end.getCharOffset() != 0) {
			TextLine endLine = view.getModel().getTextLine(end.getLineIndex());
			end = new Location(end.getLineIndex(), endLine.getEffectiveCharEndOffset(end.getCharOffset()));
		}
		highlight = new Highlight(this, start, end, style);
		view.addHighlight(highlight);
	}

	// Highlighter methods.
	
	public String getName() {
		return "Selection Highlighter";
	}
	
	/** Request to add highlights to all lines of the view from the index given onwards. */
	public int addHighlights(JTextBuffer view, int firstLineIndex) {
		if (highlight != null && isValidLocation(view, highlight.getStart()) && isValidLocation(view, highlight.getEnd())) {
			view.addHighlight(highlight);
			return 1;
		} else {
			highlight = null;
			return 0;
		}
	}
	
	private boolean isValidLocation(JTextBuffer view, Location location) {
		TextBuffer model = view.getModel();
		if (location.getLineIndex() >= model.getLineCount()) {
			return false;
		}
		TextLine line = model.getTextLine(location.getLineIndex());
		if (location.getCharOffset() >= line.length()) {
			return false;
		}
		return true;
	}

	/** Request to do something when the user clicks on a Highlight generated by this Highlighter. */
	public void highlightClicked(JTextBuffer view, Highlight highlight, String highlightedText, MouseEvent event) { }
	
	// These two should be in java.lang.Math, but they're not.
	
	public Location min(Location one, Location two) {
		return (one.compareTo(two) < 0) ? one : two;
	}
	
	public Location max(Location one, Location two) {
		return (one.compareTo(two) > 0) ? one : two;
	}
}
