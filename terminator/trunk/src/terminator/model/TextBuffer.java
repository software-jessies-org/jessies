package terminator.model;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import e.util.*;
import terminator.terminal.*;
import terminator.view.*;

/**
A TextBuffer represents all the text associated with a single connection.  It maintains a list of
TextLine objects, one for each line.

@author Phil Norman
*/

public class TextBuffer {
	private JTextBuffer view;
	private int width;
	private int height;
	private ArrayList textLines = new ArrayList();
	private short currentStyle = StyledText.getDefaultStyle();
	private int firstScrollLineIndex;
	private int lastScrollLineIndex;
	private Location cursorPosition;
	private int lastValidStartIndex = 0;
	private boolean insertMode = false;
	private ArrayList tabPositions = new ArrayList();
	private int maxLineWidth = width;
	
	// Used for reducing the number of lines changed events sent up to the view.
	private int firstLineChanged;
	
	// Fields used for saving and restoring state.
	private Location savedPosition;
	private short savedStyle;
	
	// Fields used for saving and restoring the 'real' screen while the alternative buffer is in use.
	private TextLine[] savedScreen;
	
	public TextBuffer(JTextBuffer view, int width, int height) {
		this.view = view;
		setSize(width, height);
		cursorPosition = view.getCursorPosition();
	}
	
	public void updateMaxLineWidth(int aLineWidth) {
		maxLineWidth = Math.max(getMaxLineWidth(), aLineWidth);
	}
	
	public int getMaxLineWidth() {
		return Math.max(maxLineWidth, width);
	}
	
	/** Saves the current style and location for retrieving later. */
	public void saveCursor() {
		savedPosition = cursorPosition;
		savedStyle = currentStyle;
	}
	
	/** Restores the saved style and location if it was saved earlier. */
	public void restoreCursor() {
		if (savedPosition != null) {
			cursorPosition = savedPosition;
			setStyle(savedStyle);
		}
	}

	public void checkInvariant() {
		int highestStartLineIndex = -1;
		for (int lineNumber = 0; lineNumber <= lastValidStartIndex; ++ lineNumber) {
			int thisStartLineIndex = ((TextLine) textLines.get(lineNumber)).getLineStartIndex();
			if (thisStartLineIndex <= highestStartLineIndex) {
				throw new RuntimeException("the lineStartIndex must increase monotonically as the line number increases");
			}
		}
	}
	
	public void clearScrollBuffer() {
		// FIXME: really, we should still clear everything off-screen.
		if (usingAlternativeBuffer()) {
			return;
		}
		
		// If we don't remove the highlights, we'll see the mouse
		// cursor change when we move over where they were.
		view.removeHighlightsFrom(0);
		
		// We want to keep any lines after the cursor, so remember them.
		// FIXME: if the user's editing a really long logical line at
		// the bash prompt, it may have manually wrapped it onto
		// multiple physical lines, and the cursor may not be on the
		// first of those lines. Ideally we should keep all pertinent
		// lines. Unfortunately, I can't see how we'd know.
		ArrayList retainedLines = new ArrayList(textLines.subList(cursorPosition.getLineIndex(), textLines.size()));

		// Revert to just the right number of empty lines to fill the
		// current window size.
		// Using a new ArrayList ensures we free space without risking
		// expensive nulling-out of now-unused elements. The assumption
		// being that we're most likely to be asked to clear the
		// scrollback when it's insanely large.
		Dimension oldSize = getCurrentSizeInChars();
		textLines = new ArrayList();
		setSize(width, view.getVisibleSizeInCharacters().height);
		maxLineWidth = width;
		
		// Re-insert the lines after the cursor.
		for (int i = 0; i < retainedLines.size(); ++i) {
			insertLine(i, (TextLine) retainedLines.get(i));
		}
		
		// Make sure all the lines will be redrawn.
		view.sizeChanged(oldSize, getCurrentSizeInChars());
		lineIsDirty(0);
		
		// Re-position the cursor.
		// FIXME: it's a bit crazy that these aren't tied!
		// FIXME: it's even crazier that they use different origins!
		setCursorPosition(-1, 1);
		view.setCursorPosition(cursorPosition);
		
		// Redraw ourselves.
		view.repaint();
		checkInvariant();
	}

	public void sizeChanged(Dimension sizeInChars) {
		setSize(sizeInChars.width, sizeInChars.height);
		cursorPosition = getLocationWithinBounds(cursorPosition);
		savedPosition = getLocationWithinBounds(savedPosition);
	}
	
	private Location getLocationWithinBounds(Location location) {
		if (location == null) {
			return location;
		}
		int lineIndex = Math.min(location.getLineIndex(), textLines.size() - 1);
		int charOffset = Math.min(location.getCharOffset(), width - 1);
		return new Location(lineIndex, charOffset);
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
		view.repaint();
		checkInvariant();
	}
	
	/** Returns true when the alternative buffer is in use. */
	public boolean usingAlternativeBuffer() {
		return (savedScreen != null);
	}

	public void setTabAtCursor() {
		int newPos = cursorPosition.getCharOffset();
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
		tabPositions.remove(new Integer(cursorPosition.getCharOffset()));
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
	
	/**
	* Returns a Location describing the line and offset at which the given char index exists.
	* If the index is actually larger than the screen area, returns a 'fake' location to the right
	* of the end of the last line.
	*/
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
	
	public Dimension getCurrentSizeInChars() {
		return new Dimension(getMaxLineWidth(), getLineCount());
	}
	
	public Location getCursorPosition() {
		return cursorPosition;
	}
	
	public void processActions(TerminalAction[] actions) {
		firstLineChanged = Integer.MAX_VALUE;
		boolean wereAtBottom = view.isAtBottom();
		boolean needsScroll = false;
		Dimension initialSize = getCurrentSizeInChars();
		for (int i = 0; i < actions.length; i++) {
			actions[i].perform(this);
		}
		if (firstLineChanged != Integer.MAX_VALUE) {
			needsScroll = true;
			view.linesChangedFrom(firstLineChanged);
		}
		Dimension finalSize = getCurrentSizeInChars();
		if (initialSize.equals(finalSize) == false) {
			view.sizeChanged(initialSize, finalSize);
		}
		if (needsScroll) {
			view.scrollOnTtyOutput(wereAtBottom);
		}
		view.setCursorPosition(cursorPosition);
	}
	
	public void setStyle(short style) {
		this.currentStyle = style;
	}
	
	public short getStyle() {
		return currentStyle;
	}
	
	public void moveToLine(int index) {
		if (index >= (getFirstDisplayLine() + lastScrollLineIndex)) {
			insertLine(index);
		} else {
			cursorPosition = new Location(index, cursorPosition.getCharOffset());
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
			for (int i = firstDisplayLine + lastScrollLineIndex + 1; i <= index; i++) {
				textLines.add(i, lineToInsert);
			}
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
				cursorPosition = new Location(index, cursorPosition.getCharOffset());
			}
		} else {
			textLines.remove(firstDisplayLine + lastScrollLineIndex);
			textLines.add(index, lineToInsert);
			linesChangedFrom(index);
			cursorPosition = new Location(index, cursorPosition.getCharOffset());
		}
		checkInvariant();
	}
	
	public int getFirstDisplayLine() {
		return textLines.size() - height;
	}
	
	public int getWidth() {
		return width;
	}
	
	public List/*<StyledText>*/ getLineText(int lineIndex) {
		return get(lineIndex).getStyledTextSegments();
	}
	
	public TextLine get(int index) {
		if (index >= textLines.size()) {
			Log.warn("TextLine requested for index " + index + ", size of buffer is " + textLines.size() + ".");
			return new TextLine();
		}
		return (TextLine) textLines.get(index);
	}

	public void setSize(int width, int height) {
		this.width = width;
		lineIsDirty(0);
		if (this.height > height && textLines.size() >= this.height) {
			for (int i = 0; i < (this.height - height); i++) {
				int lineToRemove = textLines.size() - 1;
				if (usingAlternativeBuffer() || (get(lineToRemove).length() == 0 && cursorPosition.getLineIndex() != lineToRemove)) {
					textLines.remove(lineToRemove);
				}
			}
		} else if (this.height < height) {
			for (int i = 0; i < (height - this.height); i++) {
				if (usingAlternativeBuffer() || getFirstDisplayLine() <= 0) {
					textLines.add(new TextLine());
				}
			}
		}
		this.height = height;
		firstScrollLineIndex = 0;
		lastScrollLineIndex = height - 1;
		while (getFirstDisplayLine() < 0) {
			textLines.add(new TextLine());
		}
		checkInvariant();
	}
	
	public void setInsertMode(boolean insertMode) {
		this.insertMode = insertMode;
	}

	/**
	 * Process the characters in the given line. The string is composed of
	 * normal printable characters, escape sequences having been extracted
	 * elsewhere.
	 */
	public void processLine(String untranslatedLine) {
		String line = view.getTerminalControl().translate(untranslatedLine);
		TextLine textLine = get(cursorPosition.getLineIndex());
		if (insertMode) {
//			Log.warn("Inserting text \"" + line + "\" at " + cursorPosition + ".");
			textLine.insertTextAt(cursorPosition.getCharOffset(), line, currentStyle);
		} else {
//			Log.warn("Writing text \"" + line + "\" at " + cursorPosition + ".");
			textLine.writeTextAt(cursorPosition.getCharOffset(), line, currentStyle);
		}
		textAdded(line.length());
	}
	
	private void textAdded(int length) {
		TextLine textLine = get(cursorPosition.getLineIndex());
		int currentLine = cursorPosition.getLineIndex();
		updateMaxLineWidth(textLine.length());
		lineIsDirty(cursorPosition.getLineIndex() + 1);  // cursorPosition's line still has a valid *start* index.
		linesChangedFrom(cursorPosition.getLineIndex());
		moveCursorHorizontally(length);
	}

	public void processSpecialCharacter(char ch) {
		switch (ch) {
		case Ascii.CR:
			cursorPosition = new Location(cursorPosition.getLineIndex(), 0);
			return;
		case Ascii.LF:
			int lineIndex = cursorPosition.getLineIndex();
			moveToLine(cursorPosition.getLineIndex() + 1);
			return;
		case Ascii.VT:
			moveCursorVertically(1);
			return;
		case Ascii.HT:
			insertTab();
			return;
		case Ascii.BS:
			moveCursorHorizontally(-1);
			return;
		default:
			Log.warn("Unsupported special character: " + ((int) ch));
		}
	}
	
	private void insertTab() {
		int nextTabLocation = getNextTabPosition(cursorPosition.getCharOffset());
		TextLine textLine = get(cursorPosition.getLineIndex());
		int startOffset = cursorPosition.getCharOffset();
		int tabLength = nextTabLocation - startOffset;
		if (insertMode) {
			textLine.insertTabAt(startOffset, tabLength, currentStyle);
		} else {
			textLine.writeTabAt(startOffset, tabLength, currentStyle);
		}
		textAdded(tabLength);
	}
	
	/** Sets whether the cursor should be visible. */
	public void setCursorVisible(boolean isDisplayed) {
		view.setCursorVisible(isDisplayed);
	}
	
	/** Inserts lines at the current cursor position. */
	public void insertLines(int count) {
		for (int i = 0; i < count; i++) {
			insertLine(cursorPosition.getLineIndex());
		}
	}
	
	public void deleteCharacters(int count) {
		TextLine line = get(cursorPosition.getLineIndex());
		int oldLineLength = line.length();
		int start = cursorPosition.getCharOffset();
		int end = start + count;
		line.killText(start, end);
		lineIsDirty(cursorPosition.getLineIndex() + 1);  // cursorPosition.y's line still has a valid *start* index.
		linesChangedFrom(cursorPosition.getLineIndex());
	}
	
	public void killHorizontally(boolean fromStart, boolean toEnd) {
		TextLine line = get(cursorPosition.getLineIndex());
		int oldLineLength = line.length();
		int start = fromStart ? 0 : cursorPosition.getCharOffset();
		int end = toEnd ? oldLineLength : cursorPosition.getCharOffset();
		line.killText(start, end);
		lineIsDirty(cursorPosition.getLineIndex() + 1);  // cursorPosition.y's line still has a valid *start* index.
		linesChangedFrom(cursorPosition.getLineIndex());
	}

	/** Erases from either the top or the cursor line, to either the bottom or the cursor line. */
	public void killVertically(boolean fromTop, boolean toBottom) {
		int start = fromTop ? getFirstDisplayLine() : cursorPosition.getLineIndex();
		int end = toBottom ? getLineCount() : cursorPosition.getLineIndex();
		for (int i = start; i < end; i++) {
			get(i).clear();
		}
		if (fromTop && toBottom) {
			setCursorPosition(1, 1);  // Clear screen also implies moving the cursor to 'home'.
		}
		lineIsDirty(start + 1);
		view.repaint();
	}
	
	/**
	* Sets the position of the cursor to the given x and y coordinates, counted from 1,1 at the top-left corner.
	* If either x or y is -1, that coordinate is left unchanged.
	*/
	public void setCursorPosition(int x, int y) {
		// Although the cursor positions are supposed to be measured
		// from (1,1), there's nothing to stop a badly-behaved program
		// from sending (0,0). ASUS routers do this (they're rubbish).
		// Note that here we also transform from 1-based coordinates to 0-based.
		x = (x == -1) ? cursorPosition.getCharOffset() : Math.max(0, x - 1);
		y = (y == -1) ? cursorPosition.getLineIndex() : Math.max(0, y - 1);
		x = Math.min(x, width - 1);
		y = Math.min(y, height - 1);

		cursorPosition = new Location(y + getFirstDisplayLine(), x);
	}
	
	/** Moves the cursor horizontally by the number of characters in xDiff, negative for left, positive for right. */
	public void moveCursorHorizontally(int xDiff) {
		int charOffset = cursorPosition.getCharOffset() + xDiff;
		int lineIndex = cursorPosition.getLineIndex();
		while (charOffset < 0) {
			TextLine lineAbove = get(--lineIndex);
			charOffset += lineAbove.length();
		}
		cursorPosition = new Location(lineIndex, charOffset);
	}
	
	/** Moves the cursor vertically by the number of characters in yDiff, negative for up, positive for down. */
	public void moveCursorVertically(int yDiff) {
		cursorPosition = new Location(cursorPosition.getLineIndex() + yDiff, cursorPosition.getCharOffset());
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
		linesChangedFrom(addIndex);
		view.repaint();
		checkInvariant();
	}

	/** Scrolls the display down by one line. */
	public void scrollDisplayDown() {
		int removeIndex = getFirstDisplayLine() + firstScrollLineIndex;
		int addIndex = getFirstDisplayLine() + lastScrollLineIndex + 1;
		textLines.add(addIndex, new TextLine());
		textLines.remove(removeIndex);
		lineIsDirty(removeIndex);
		linesChangedFrom(removeIndex);
		view.repaint();
		checkInvariant();
	}
	
	public void setWindowTitle(String newWindowTitle) {
		JTerminalPane terminalPane = (JTerminalPane) SwingUtilities.getAncestorOfClass(JTerminalPane.class, view);
		terminalPane.setName(newWindowTitle);
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
				String str = line.getText() + '\n';
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
