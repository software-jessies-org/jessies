package terminator.view.highlight;

import e.util.*;
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
			super(null, null, null, null, false);
		}
		
		@Override
		public Color getBackground() {
			return (focused ? Options.getSharedInstance().getColor("selectionColor") : unfocusedSelectionColor);
		}
		
		@Override
		public Color getForeground() {
			return Options.getSharedInstance().getColor("foreground");
		}
		
		@Override
		public boolean hasBackground() {
			return true;
		}
		
		@Override
		public boolean hasForeground() {
			return true;
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
	
	private TerminalView view;
	private Highlight highlight;
	private Location startLocation;
	private DragHandler dragHandler;
	
	/** Creates a SelectionHighlighter for selecting text in the given view, and adds us as mouse listeners to that view. */
	public SelectionHighlighter(TerminalView view) {
		this.view = view;
		view.addFocusListener(style);
		view.addMouseListener(this);
		view.addMouseMotionListener(this);
		view.addHighlighter(this);
		view.setAutoscrolls(true);
	}
	
	public void textChanged(Location start, Location end) {
		if (highlight != null) {
			if ((highlight.getStart().compareTo(end) < 0) && (highlight.getEnd().compareTo(start) > 0)) {
				view.removeHighlightsFrom(this, 0);
				highlight = null;
			}
		}
	}
	
	public void mousePressed(MouseEvent e) {
		if (e.isConsumed() || SwingUtilities.isLeftMouseButton(e) == false || e.isPopupTrigger()) {
			return;
		}
		
		// Shift-click should move one end of the selection.
		if (e.isShiftDown() && startLocation != null) {
			Location newEndLocation = view.viewToModel(e.getPoint());
			dragHandler = new SingleClickDragHandler();
			// The following line does nothing, but we should call makeInitialSelection to
			// protect against future changes in implementation.
			dragHandler.makeInitialSelection(startLocation);
			dragHandler.mouseDragged(newEndLocation);
			return;
		}
		
		Location loc = view.viewToModel(e.getPoint());
		view.removeHighlightsFrom(this, 0);
		highlight = null;
		startLocation = loc;
		
		if (loc.getLineIndex() >= view.getModel().getLineCount()) {
			return;
		}
		dragHandler = getDragHandlerForClick(e);
		dragHandler.makeInitialSelection(loc);
	}
	
	public void mouseClicked(MouseEvent event) {
		if (event.getButton() == MouseEvent.BUTTON2) {
			view.middleButtonPaste();
		}
	}
	
	public void mouseReleased(MouseEvent event) {
		dragHandler = null;
	}
	
	public void mouseDragged(MouseEvent event) {
		if (SwingUtilities.isLeftMouseButton(event) && (dragHandler != null)) {
			Location loc = view.viewToModel(event.getPoint());
			dragHandler.mouseDragged(loc);
			view.scrollRectToVisible(new Rectangle(event.getX(), event.getY(), 10, 10));
		}
	}
	
	private DragHandler getDragHandlerForClick(MouseEvent e) {
		if (e.getClickCount() == 1) {
			return new SingleClickDragHandler();
		} else if (e.getClickCount() == 2) {
			return new DoubleClickDragHandler();
		} else {
			return new TripleClickDragHandler();
		}
	}
	
	public void mouseMoved(MouseEvent event) { }
	
	public void mouseEntered(MouseEvent event) { }
	
	public void mouseExited(MouseEvent event) { }
	
	public interface DragHandler {
		public void makeInitialSelection(Location pressedLocation);
		public void mouseDragged(Location newLocation);
	}
	
	public class SingleClickDragHandler implements DragHandler {
		public void makeInitialSelection(Location pressedLocation) { }
		
		public void mouseDragged(Location newLocation) {
			setHighlight(min(startLocation, newLocation), max(startLocation, newLocation));
		}
	}
	
	public class DoubleClickDragHandler implements DragHandler {
		public void makeInitialSelection(Location pressedLocation) {
			setHighlight(getWordStart(pressedLocation), getWordEnd(pressedLocation));
		}
		
		public void mouseDragged(Location newLocation) {
			Location start = min(startLocation, newLocation);
			Location end = max(startLocation, newLocation);
			setHighlight(getWordStart(start), getWordEnd(end));
		}
		
		private Location getWordStart(Location location) {
			final int lineNumber = location.getLineIndex();
			String line = view.getModel().getTextLine(lineNumber).getString();
			if (location.getCharOffset() >= line.length()) {
				return location;
			}
			
			int start = location.getCharOffset();
			while (start > 0 && isWordChar(line.charAt(start - 1))) {
				--start;
			}
			return new Location(lineNumber, start);
		}
		
		private Location getWordEnd(Location location) {
			final int lineNumber = location.getLineIndex();
			String line = view.getModel().getTextLine(lineNumber).getString();
			if (location.getCharOffset() >= line.length()) {
				return location;
			}
			
			int end = location.getCharOffset();
			while (end < line.length() && isWordChar(line.charAt(end))) {
				++end;
			}
			return new Location(lineNumber, end);
		}
		
		private boolean isWordChar(char ch) {
			// Space marks the end of a word by any reasonable definition.
			// Bracket characters usually mark the end of what you're interested in.
			// Likewise quote characters.
			return " <>(){}[]`'\"".indexOf(ch) == -1;
		}
	}
	
	public class TripleClickDragHandler implements DragHandler {
		public void makeInitialSelection(Location pressedLocation) {
			selectLines(pressedLocation, pressedLocation);
		}
		
		public void mouseDragged(Location newLocation) {
			Location start = min(startLocation, newLocation);
			Location end = max(startLocation, newLocation);
			selectLines(start, end);
		}
		
		private void selectLines(Location start, Location end) {
			start = new Location(start.getLineIndex(), 0);
			end = new Location(end.getLineIndex() + 1, 0);
			setHighlight(start, end);
		}
	}
	
	private void clearSelection() {
		view.removeHighlightsFrom(this, 0);
		startLocation = null;
		highlight = null;
	}
	
	public void selectAll() {
		Location start = new Location(0, 0);
		Location end = new Location(view.getModel().getLineCount(), 0);
		setHighlight(start, end);
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
	public void updateSystemSelection() {
		if (highlight == null) {
			// Almost all X11 applications leave the selection alone in this case.
			return;
		}
		Clipboard systemSelection = view.getToolkit().getSystemSelection();
		if (systemSelection != null) {
			systemSelection.setContents(new LazyStringSelection() {
				public String reallyGetText() {
					return getTabbedString();
				}
			}, this);
		}
	}
	
	/**
	 * Copies the selected text to X11's selection (like XTerm and friends) or Windows's clipboard (like PuTTY).
	 */
	private void selectionChanged() {
		if (e.util.GuiUtilities.isWindows()) {
			copyToSystemClipboard();
		} else {
			updateSystemSelection();
		}
	}
	
	private void copyToClipboard(Clipboard clipboard) {
		if (highlight == null) {
			return;
		}
		String newContents = getTabbedString();
		if (newContents.length() == 0) {
			// Copying the empty string to the clipboard is bizarre, and caused one user trouble (because we didn't cope with zero-length pastes).
			return;
		}
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
	
	private void setHighlight(Location start, Location end) {
		view.removeHighlightsFrom(this, 0);
		TextLine startLine = view.getModel().getTextLine(start.getLineIndex());
		start = new Location(start.getLineIndex(), startLine.getEffectiveCharStartOffset(start.getCharOffset()));
		// Cope with selections off the bottom of the screen.
		if (end.getCharOffset() != 0) {
			TextLine endLine = view.getModel().getTextLine(end.getLineIndex());
			end = new Location(end.getLineIndex(), endLine.getEffectiveCharEndOffset(end.getCharOffset()));
		}
		if (start.equals(end)) {
			highlight = null;
		} else {
			highlight = new Highlight(this, start, end, style);
			view.addHighlight(highlight);
			selectionChanged();
		}
	}

	// Highlighter methods.
	
	public String getName() {
		return "Selection Highlighter";
	}
	
	/** Request to add highlights to all lines of the view from the index given onwards. */
	public int addHighlights(TerminalView view, int firstLineIndex) {
		if (highlight != null && isValidLocation(view, highlight.getStart()) && isValidLocation(view, highlight.getEnd())) {
			view.addHighlight(highlight);
			return 1;
		} else {
			highlight = null;
			return 0;
		}
	}
	
	private boolean isValidLocation(TerminalView view, Location location) {
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
	public void highlightClicked(TerminalView view, Highlight highlight, String highlightedText, MouseEvent event) { }
	
	// These two should be in java.lang.Math, but they're not.
	
	public Location min(Location one, Location two) {
		return (one.compareTo(two) < 0) ? one : two;
	}
	
	public Location max(Location one, Location two) {
		return (one.compareTo(two) > 0) ? one : two;
	}
}
