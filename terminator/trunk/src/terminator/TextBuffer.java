package terminatorn;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
A TextBuffer represents all the text associated with a single connection.  It maintains a list of
TextLine objects, one for each line.

@author Phil Norman
*/

public class TextBuffer implements TelnetListener {
	private JTextBuffer view;
	private int width;
	private int height;
	private ArrayList textLines = new ArrayList();
	private int currentStyle = StyledText.getDefaultStyle();
	private int firstScrollLineIndex;
	private int lastScrollLineIndex;
	private Location caretPosition;
	private int lastValidStartIndex = 0;
	private boolean insertMode = false;
	
	// Fields used for saving and restoring state.
	private Location savedPosition;
	private int savedStyle;
	
	// Fields used for saving and restoring the 'real' screen while the alternative buffer is in use.
	private TextLine[] savedScreen;
	
	public TextBuffer(JTextBuffer view, int width, int height) {
		this.view = view;
		setSize(width, height);
		caretPosition = view.getCaretPosition();
	}
	
	/** Saves the current style and location for retrieving later. */
	public void saveCursor() {
		savedPosition = caretPosition;
		savedStyle = currentStyle;
	}
	
	/** Restores the saved style and location if it was saved earlier. */
	public void restoreCursor() {
		if (savedPosition != null) {
			caretPosition = savedPosition;
			setStyle(savedStyle);
		}
	}
	
	/** Sets or unsets the use of the alternative buffer. */
	public void useAlternativeBuffer(boolean useAlternativeBuffer) {
		if (useAlternativeBuffer == usingAlternativeBuffer()) {
			return;
		}
		if (useAlternativeBuffer) {
			savedScreen = new TextLine[height];
			for (int i = 0; i < height; i++) {
				int lineIndex = getFirstDisplayLine() + i;
				savedScreen[i] = get(lineIndex);
				textLines.set(lineIndex, new TextLine());
			}
		} else {
			for (int i = 0; i < height; i++) {
				int lineIndex = getFirstDisplayLine() + i;
				textLines.set(lineIndex, savedScreen[i]);
			}
			savedScreen = null;
		}
		lineIsDirty(getFirstDisplayLine());
	}
	
	/** Returns true when the alternative buffer is in use. */
	private boolean usingAlternativeBuffer() {
		return (savedScreen != null);
	}
	
	/** Returns the contents of the indexed line excluding the terminating NL. */
	public String getLine(int lineIndex) {
		return get(lineIndex).getText();
	}
	
	/** Returns the length of the indexed line including the terminating NL. */
	public int getLineLength(int lineIndex) {
		return get(lineIndex).length() + 1;
	}
	
	/** Returns the start character index of the indexed line. */
	public int getStartIndex(int lineIndex) {
		ensureValidStartIndex(lineIndex);
		return get(lineIndex).getLineStartIndex();
	}
	
	/** Returns a Location describing the line and offset at which the given char index exists. */
	public Location getLocationFromCharIndex(int charIndex) {
		int lowLine = 0;
		int low = 0;
		int highLine = textLines.size();
		int high = getStartIndex(highLine - 1) + getLineLength(highLine - 1);
		
		while (highLine - lowLine > 1) {
			int midLine = (lowLine + highLine) / 2;
			int mid = getStartIndex(midLine);
			if (mid <= charIndex) {
				lowLine = midLine;
				low = mid;
			} else {
				highLine = midLine;
				high = mid;
			}
		}
		return new Location(lowLine, charIndex - getStartIndex(lowLine));
	}
	
	/** Returns the char index equivalent to the given Location. */
	public int getCharIndexFromLocation(Location location) {
		return getStartIndex(location.getLineIndex()) + location.getCharOffset();
	}
	
	/** Returns the count of all characters in the buffer, including NLs. */
	public int length() {
		int lastIndex = textLines.size() - 1;
		return getStartIndex(lastIndex) + getLineLength(lastIndex);
	}
	
	public CharSequence getCharSequence() {
		return getCharSequence(0, length());
	}
	
	public CharSequence getCharSequence(Location start, Location end) {
		return getCharSequence(getCharIndexFromLocation(start), getCharIndexFromLocation(end));
	}
	
	public CharSequence getCharSequence(int startIndex, int endIndex) {
		return new Sequence(startIndex, endIndex);
	}
	
	private void lineIsDirty(int dirtyLineIndex) {
		lastValidStartIndex = Math.min(lastValidStartIndex, dirtyLineIndex + 1);
	}
	
	private void ensureValidStartIndex(int lineIndex) {
		if (lineIndex > lastValidStartIndex) {
			for (int i = lastValidStartIndex; i < lineIndex; i++) {
				TextLine line = get(i);
				get(i + 1).setLineStartIndex(line.getLineStartIndex() + line.length() + 1);
			}
			lastValidStartIndex = lineIndex;
		}
	}
	
	public int getLineCount() {
		return textLines.size();
	}
	
	public void fullReset() {
		int firstLineToClear = getFirstDisplayLine();
		for (int i = 0; i < height; i++) {
			get(firstLineToClear + i).clear();
		}
		view.repaint();
	}
	
	public void processActions(TelnetAction[] actions) {
		boolean wereAtBottom = view.isAtBottom();
		int initialLineCount = getLineCount();
		for (int i = 0; i < actions.length; i++) {
			actions[i].perform(this);
		}
		if (getLineCount() != initialLineCount) {
			view.sizeChanged();
		}
		view.scrollOnTtyOutput(wereAtBottom);
		view.setCaretPosition(caretPosition);
	}
	
	public void setStyle(int style) {
		this.currentStyle = style;
	}
	
	public int getStyle() {
		return currentStyle;
	}
	
	public void moveToLine(int index) {
		if (index >= (getFirstDisplayLine() + lastScrollLineIndex)) {
			insertLine(index);
		} else {
			caretPosition = new Location(index, caretPosition.getCharOffset());
		}
	}

	public void insertLine(int index) {
		// Use a private copy of the first display line throughout this method to avoid mutation
		// caused by textLines.add()/textLines.remove().
		final int firstDisplayLine = getFirstDisplayLine();
		lineIsDirty(firstDisplayLine);
		if (index > firstDisplayLine + lastScrollLineIndex) {
			textLines.add(index, new TextLine());
			if (usingAlternativeBuffer() || (firstScrollLineIndex > 0)) {
				// If the program has defined scroll bounds, newline-adding actually chucks away
				// the first scroll line, rather than just scrolling everything upwards like we normally
				// do.  This makes vim work better.  Also, if we're using the alternative buffer, we
				// don't add anything going off the top into the history.
				textLines.remove(firstDisplayLine + firstScrollLineIndex);
				view.repaint();
			} else {
				caretPosition = new Location(index, caretPosition.getCharOffset());
			}
		} else {
			textLines.remove(firstDisplayLine + lastScrollLineIndex);
			textLines.add(index, new TextLine());
			caretPosition = new Location(index, caretPosition.getCharOffset());
		}
	}
	
	private int getFirstDisplayLine() {
		return textLines.size() - height;
	}
	
	public int getWidth() {
		return width;
	}
	
	public StyledText[] getLineText(int lineIndex) {
		return get(lineIndex).getStyledTextSegments();
	}
	
	public TextLine get(int index) {
		return (TextLine) textLines.get(index);
	}

	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
		firstScrollLineIndex = 0;
		lastScrollLineIndex = height - 1;
		while (textLines.size() < height) {
			textLines.add(new TextLine());
		}
	}
	
	public void setInsertMode(boolean insertMode) {
		this.insertMode = insertMode;
	}

	public void processLine(String line) {
		TextLine textLine = get(caretPosition.getLineIndex());
		if (insertMode) {
			textLine.insertTextAt(caretPosition.getCharOffset(), line, currentStyle);
		} else {
			textLine.writeTextAt(caretPosition.getCharOffset(), line, currentStyle);
		}
		lineIsDirty(caretPosition.getLineIndex() + 1);  // caretPosition's line still has a valid *start* index.
		int lastCharChanged = insertMode ? textLine.length() : caretPosition.getCharOffset() + line.length();
		view.lineSectionChanged(caretPosition.getLineIndex(), caretPosition.getCharOffset(), lastCharChanged);
		moveCursorHorizontally(line.length());
	}

	public void processSpecialCharacter(char ch) {
		switch (ch) {
			case '\r': caretPosition = new Location(caretPosition.getLineIndex(), 0); return;
			case '\n': moveToLine(caretPosition.getLineIndex() + 1); return;
			case '\t': insertTab(); return;
			case KeyEvent.VK_BACK_SPACE: moveCursorHorizontally(-1); return;
			default: Log.warn("Unsupported special character: " + ((int) ch));
		}
	}
	
	private void insertTab() {
		// FIXME: Need to cope with modification of tab positions.
		// For now we just assume one tab every 8 spaces.
		int nextTabLocation = (caretPosition.getCharOffset() + 8) & ~7;
		char[] spaces = new char[nextTabLocation - caretPosition.getCharOffset()];
		Arrays.fill(spaces, ' ');
		processLine(new String(spaces));
	}
	
	/** Sets whether the caret should be displayed. */
	public void setCaretDisplay(boolean isDisplayed) {
		view.setCaretDisplay(isDisplayed);
	}
	
	/** Inserts lines at the current caret position. */
	public void insertLines(int count) {
		for (int i = 0; i < count; i++) {
			insertLine(caretPosition.getLineIndex());
		}
	}
	
	public void deleteCharacters(int count) {
		TextLine line = get(caretPosition.getLineIndex());
		int oldLineLength = line.length();
		int start = caretPosition.getCharOffset();
		int end = start + count;
		line.killText(start, end);
		lineIsDirty(caretPosition.getLineIndex() + 1);  // caretPosition.y's line still has a valid *start* index.
		view.lineSectionChanged(caretPosition.getLineIndex(), 0, oldLineLength);
	}
	
	public void killHorizontally(boolean fromStart, boolean toEnd) {
		TextLine line = get(caretPosition.getLineIndex());
		int oldLineLength = line.length();
		int start = fromStart ? 0 : caretPosition.getCharOffset();
		int end = toEnd ? oldLineLength : caretPosition.getCharOffset();
		line.killText(start, end);
		lineIsDirty(caretPosition.getLineIndex() + 1);  // caretPosition.y's line still has a valid *start* index.
		view.lineSectionChanged(caretPosition.getLineIndex(), 0, oldLineLength);
	}

	/** Erases from either the top or the cursor line, to either the bottom or the cursor line. */
	public void killVertically(boolean fromTop, boolean toBottom) {
		int start = fromTop ? getFirstDisplayLine() : caretPosition.getLineIndex();
		int end = toBottom ? getLineCount() : caretPosition.getLineIndex();
		for (int i = start; i < end; i++) {
			get(i).clear();
		}
		if (fromTop && toBottom) {
			setCursorPosition(1, 1);  // Clear screen also implies moving the cursor to 'home'.
		}
		lineIsDirty(start + 1);
		view.repaint();
	}
	
	/** Sets the position of the cursor to the given x and y coordinates, counted from 1,1 at the top-left corner. */
	public void setCursorPosition(int x, int y) {
		// Although the cursor positions are supposed to be measured
		// from (1,1), there's nothing to stop a badly-behaved program
		// from sending (0,0). ASUS routers do this (they're rubbish).
		x = Math.max(1, x);
		y = Math.max(1, y);

		caretPosition = new Location(y - 1 + getFirstDisplayLine(), x - 1);
	}
	
	/** Moves the cursor horizontally by the number of characters in xDiff, negative for left, positive for right. */
	public void moveCursorHorizontally(int xDiff) {
		caretPosition = new Location(caretPosition.getLineIndex(), caretPosition.getCharOffset() + xDiff);
	}
	
	/** Moves the cursor vertically by the number of characters in yDiff, negative for up, positive for down. */
	public void moveCursorVertically(int yDiff) {
		caretPosition = new Location(caretPosition.getLineIndex() + yDiff, caretPosition.getCharOffset());
	}

	/** Sets the first and last lines to scroll.  If both are -1, make the entire screen scroll. */
	public void setScrollScreen(int firstLine, int lastLine) {
		firstScrollLineIndex = ((firstLine == -1) ? 1 : firstLine) - 1;
		lastScrollLineIndex = ((lastLine == -1) ? height : lastLine) - 1;
	}

	/** Scrolls the display up by one line. */
	public void scrollDisplayUp() {
		int addIndex = getFirstDisplayLine() + firstScrollLineIndex;
		int removeIndex = getFirstDisplayLine() + lastScrollLineIndex + 1;
		textLines.add(addIndex, new TextLine());
		textLines.remove(removeIndex);
		lineIsDirty(addIndex);
		view.repaint();
	}

	/** Scrolls the display down by one line. */
	public void scrollDisplayDown() {
		int removeIndex = getFirstDisplayLine() + firstScrollLineIndex;
		int addIndex = getFirstDisplayLine() + lastScrollLineIndex + 1;
		textLines.add(addIndex, new TextLine());
		textLines.remove(removeIndex);
		lineIsDirty(removeIndex);
		view.repaint();
	}
	
	public void setWindowTitle(String newWindowTitle) {
		// What we do here depends on whether there are multiple tabs.
		// I'm unconvinced this is the right place/way to handle this, but nothing else springs to mind at the moment.
		JTabbedPane tabbedPane = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, view);
		if (tabbedPane != null) {
			// Find the tab we're in, and change its title.
			JTelnetPane telnetPane = (JTelnetPane) SwingUtilities.getAncestorOfClass(JTelnetPane.class, view);
			int index = tabbedPane.indexOfComponent(telnetPane);
			tabbedPane.setTitleAt(index, newWindowTitle);
		} else {
			// Find the frame we're in, and change its title.
			JFrame frame= (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, view);
			frame.setTitle(newWindowTitle);
		}
	}
	
	public class Sequence implements CharSequence {
		private int start;
		private int end;
		
		public Sequence(int start, int end) {
			this.start = start;
			this.end = end;
		}
		
		public char charAt(int index) {
			Location loc = getLocationFromCharIndex(start + index);
			String line = getLine(loc.getLineIndex());
			if (line.length() > loc.getCharOffset()) {
				return line.charAt(loc.getCharOffset());
			} else {
				return '\n';  // The only case of a character indexable but not appearing in the string.
			}
		}
		
		public int length() {
			return end - start;
		}
		
		public CharSequence subSequence(int subStart, int subEnd) {
			return new Sequence(start + subStart, start + subEnd);
		}
		
		public String toString() {
			StringBuffer buf = new StringBuffer(length());
			Location loc = getLocationFromCharIndex(start);
			int charsLeft = end - start;
			while (charsLeft > 0) {
				String str = getLine(loc.getLineIndex()).substring(loc.getCharOffset());
				if (charsLeft < str.length()) {
					str = str.substring(0, charsLeft);
				} else if (charsLeft > str.length()) {
					str += '\n';
				}
				buf.append(str);
				charsLeft -= str.length();
				loc = new Location(loc.getLineIndex() + 1, 0);
			}
			return buf.toString();
		}
	}
}
