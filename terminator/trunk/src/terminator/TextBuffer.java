package terminatorn;

import java.awt.*;
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
	private ArrayList tabPositions = new ArrayList();
	
	// Used for reducing the number of lines changed events sent up to the view.
	private int firstLineChanged;
	
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
	
	public void reset() {
		// Revert to just the right number of empty lines to fill the
		// current window size.
		textLines = new ArrayList();
		setSize(width, view.getVisibleSizeInCharacters().height);
		
		// Make sure all the lines will be redrawn.
		view.sizeChanged();
		lineIsDirty(0);
		
		// Home the cursor.
		// FIXME: it's a bit crazy that these aren't tied!
		// FIXME: it's even crazier that they use different origins!
		setCursorPosition(1, 1);
		view.setCaretPosition(new Location(0, 0));
		
		// Redraw ourselves.
		view.repaint();
		
		// FIXME: what we really want to do now is send SIGWINCH so the
		// running application redraws itself.
	}

	public void sizeChanged(Dimension sizeInChars) {
		setSize(sizeInChars.width, sizeInChars.height);
		int caretCharIndex = getCharIndexFromLocation(caretPosition);
		ArrayList newLines = new ArrayList();
		TextLine currentLine = null;
		for (int i = 0; i < textLines.size(); i++) {
			if (currentLine == null) {
				currentLine = (TextLine) textLines.get(i);
			} else {
				currentLine.append((TextLine) textLines.get(i));
			}
			while (currentLine.length() > width) {
				TextLine after = currentLine.splitAt(width);
				newLines.add(currentLine);
				currentLine = after;
			}
			if (currentLine.hasNewline() || (i == textLines.size() - 1)) {
				newLines.add(currentLine);
				currentLine = null;
			}
		}
		textLines = newLines;
		caretPosition = getLocationFromCharIndex(caretCharIndex);
		
		// Send a size change even if it hasn't, because it also causes a complete redraw
		// and higlight recalculation.
		view.setCaretPosition(caretPosition);
		view.sizeChanged();
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
				textLines.set(lineIndex, i >= savedScreen.length ? new TextLine() : savedScreen[i]);
			}
			for (int i = height; i < savedScreen.length; i++) {
				textLines.add(savedScreen[i]);
			}
			savedScreen = null;
		}
		lineIsDirty(getFirstDisplayLine());
		for (int i = 0; i < height; i++) {
			int index = getFirstDisplayLine() + i;
			linesChangedFrom(index);
		}
	}
	
	/** Returns true when the alternative buffer is in use. */
	private boolean usingAlternativeBuffer() {
		return (savedScreen != null);
	}

	public void setTabAtCursor() {
		int newPos = caretPosition.getCharOffset();
		for (int i = 0; i < tabPositions.size(); i++) {
			int pos = ((Integer) tabPositions.get(i)).intValue();
			if (pos == newPos) {
				return;
			} else if (pos > newPos) {
				tabPositions.add(i, new Integer(newPos));
				return;
			}
		}
		tabPositions.add(new Integer(newPos));
	}
	
	public void removeTabAtCursor() {
		tabPositions.remove(new Integer(caretPosition.getCharOffset()));
	}
	
	public void removeAllTabs() {
		tabPositions.clear();
	}
	
	private int getNextTabPosition(int charOffset) {
		for (int i = 0; i < tabPositions.size(); i++) {
			int pos = ((Integer) tabPositions.get(i)).intValue();
			if (pos > charOffset) {
				return pos;
			}
		}
		// No special tab to our right; return the default 8-separated tab stop.
		return (charOffset + 8) & ~7;
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
				get(i + 1).setLineStartIndex(line.getLineStartIndex() + line.lengthIncludingNewline());
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
	
	public void linesChangedFrom(int firstLineChanged) {
		this.firstLineChanged = Math.min(this.firstLineChanged, firstLineChanged);
	}
	
	public void processActions(TelnetAction[] actions) {
		firstLineChanged = Integer.MAX_VALUE;
		boolean wereAtBottom = view.isAtBottom();
		int initialLineCount = getLineCount();
		for (int i = 0; i < actions.length; i++) {
			actions[i].perform(this);
		}
		if (firstLineChanged != Integer.MAX_VALUE) {
			view.linesChangedFrom(firstLineChanged);
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
		insertLine(index, new TextLine());
	}

	public void insertLine(int index, TextLine lineToInsert) {
		// Use a private copy of the first display line throughout this method to avoid mutation
		// caused by textLines.add()/textLines.remove().
		final int firstDisplayLine = getFirstDisplayLine();
		lineIsDirty(firstDisplayLine);
		if (index > firstDisplayLine + lastScrollLineIndex) {
			textLines.add(index, lineToInsert);
			if (usingAlternativeBuffer() || (firstScrollLineIndex > 0)) {
				// If the program has defined scroll bounds, newline-adding actually chucks away
				// the first scroll line, rather than just scrolling everything upwards like we normally
				// do.  This makes vim work better.  Also, if we're using the alternative buffer, we
				// don't add anything going off the top into the history.
				int removeIndex = firstDisplayLine + firstScrollLineIndex;
				textLines.remove(removeIndex);
				linesChangedFrom(removeIndex);
				view.repaint();
			} else {
				caretPosition = new Location(index, caretPosition.getCharOffset());
			}
		} else {
			textLines.remove(firstDisplayLine + lastScrollLineIndex);
			textLines.add(index, lineToInsert);
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
		if (this.height > height) {
//			if (caretPosition.getLineIndex() >= getFirstDisplayLine() + height) {
				Log.warn("Moving caret out of harm's way.");
				int newLineIndex = Math.max(getFirstDisplayLine(), caretPosition.getLineIndex() + height - this.height);
				caretPosition = new Location(newLineIndex, caretPosition.getCharOffset());
//			}
			for (int i = 0; i < (this.height - height); i++) {
				textLines.remove(textLines.size() - 1);
			}
		} else if (this.height < height) {
			for (int i = 0; i < (height - this.height); i++) {
				textLines.add(new TextLine());
			}
		}
		this.height = height;
		firstScrollLineIndex = 0;
		lastScrollLineIndex = height - 1;
		while (textLines.size() - getFirstDisplayLine() < height) {
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
		textAdded(line.length());
	}
	
	private void textAdded(int length) {
		TextLine textLine = get(caretPosition.getLineIndex());
		int currentLine = caretPosition.getLineIndex();
		while (textLine.length() > width) {
			insertLine(currentLine + 1, textLine.splitAt(width));
		}
		lineIsDirty(caretPosition.getLineIndex() + 1);  // caretPosition's line still has a valid *start* index.
		linesChangedFrom(caretPosition.getLineIndex());
		moveCursorHorizontally(length);
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
		int nextTabLocation = getNextTabPosition(caretPosition.getCharOffset());
		TextLine textLine = get(caretPosition.getLineIndex());
		int startOffset = caretPosition.getCharOffset();
		int tabLength = nextTabLocation - startOffset;
		if (insertMode) {
			textLine.insertTabAt(startOffset, tabLength, currentStyle);
		} else {
			textLine.writeTabAt(startOffset, tabLength, currentStyle);
		}
		textAdded(tabLength);
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
		linesChangedFrom(caretPosition.getLineIndex());
	}
	
	public void killHorizontally(boolean fromStart, boolean toEnd) {
		TextLine line = get(caretPosition.getLineIndex());
		int oldLineLength = line.length();
		int start = fromStart ? 0 : caretPosition.getCharOffset();
		int end = toEnd ? oldLineLength : caretPosition.getCharOffset();
		line.killText(start, end);
		lineIsDirty(caretPosition.getLineIndex() + 1);  // caretPosition.y's line still has a valid *start* index.
		linesChangedFrom(caretPosition.getLineIndex());
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
		int charOffset = caretPosition.getCharOffset() + xDiff;
		int lineIndex = caretPosition.getLineIndex();
		while (charOffset < 0) {
			TextLine lineAbove = get(--lineIndex);
			charOffset += lineAbove.length();
		}
		caretPosition = new Location(lineIndex, charOffset);
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
			String line = get(loc.getLineIndex()).getText();
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
				TextLine line = get(loc.getLineIndex());
				String str = line.hasNewline() ? line.getText() + '\n' : line.getText();
				str = str.substring(loc.getCharOffset());
				if (charsLeft < str.length()) {
					str = str.substring(0, charsLeft);
				}
				buf.append(str);
				charsLeft -= str.length();
				loc = new Location(loc.getLineIndex() + 1, 0);
			}
			return buf.toString();
		}
	}
}
