package terminatorn;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

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
	
	public TextBuffer(JTextBuffer view, int width, int height) {
		this.view = view;
		setSize(width, height);
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
	
	public void setStyle(int style) {
		this.currentStyle = style;
	}

	public void insertLine(int index) {
		// Use a private copy of the first display line throughout this method to avoid mutation
		// caused by textLines.add()/textLines.remove().
		final int firstDisplayLine = getFirstDisplayLine();
		if (index >= firstDisplayLine + lastScrollLineIndex) {
			textLines.add(index, new TextLine());
			if (firstScrollLineIndex == 0) {
				view.setCaretPosition(new Point(0, index));
				view.sizeChanged();
			} else {
				// If the program's defined scroll bounds, newline-adding actually chucks away
				// the first scroll line, rather than just scrolling everything upwards like we normally
				// do.  This makes vim work better.
				textLines.remove(firstDisplayLine + firstScrollLineIndex);
				view.repaint();
			}
		} else {
			view.setCaretPosition(new Point(0, index));
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

	public void processLine(final String line) {
		for (int i = 0; i < line.length(); i++) {
			Point pos = view.getCaretPosition();
			char ch = line.charAt(i);
			if (ch == KeyEvent.VK_BACK_SPACE) {
				moveCursorHorizontally(-1);
			} else if (ch == '\r') {
				view.setCaretPosition(new Point(0, pos.y));
			} else if (ch == '\n') {
				insertLine(pos.y + 1);
//				moveCursorVertically(1);
			} else {
				get(pos.y).writeTextAt(pos.x, new String(new char[] { ch }), currentStyle);
				view.lineSectionChanged(pos.y, pos.x, pos.x + 1);
				moveCursorHorizontally(1);
			}
		}
	}
	
	public void killHorizontally(boolean fromStart, boolean toEnd) {
		Point caretPos = view.getCaretPosition();
		TextLine line = get(caretPos.y);
		int oldLineLength = line.length();
		int start = fromStart ? 0 : caretPos.x;
		int end = toEnd ? oldLineLength : caretPos.x;
		line.killText(start, end);
		view.lineSectionChanged(caretPos.y, 0, oldLineLength);
	}

	/** Erases from either the top or the cursor line, to either the bottom or the cursor line. */
	public void killVertically(boolean fromTop, boolean toBottom) {
		Point caretPos = view.getCaretPosition();
		int start = fromTop ? getFirstDisplayLine() : caretPos.y;
		int end = toBottom ? getLineCount() : caretPos.y;
		for (int i = start; i < end; i++) {
			get(i).clear();
		}
		if (fromTop && toBottom) {
			setCursorPosition(1, 1);  // Clear screen also implies moving the cursor to 'home'.
		}
		view.repaint();
	}
	
	/** Sets the position of the cursor to the given x and y coordinates, counted from 1,1 at the top-left corner. */
	public void setCursorPosition(int x, int y) {
		view.setCaretPosition(new Point(x - 1, y - 1 + getFirstDisplayLine()));
	}
	
	/** Moves the cursor horizontally by the number of characters in xDiff, negative for left, positive for right. */
	public void moveCursorHorizontally(int xDiff) {
		moveCursor(xDiff, 0);
	}
	
	/** Moves the cursor vertically by the number of characters in yDiff, negative for up, positive for down. */
	public void moveCursorVertically(int yDiff) {
		moveCursor(0, yDiff);
	}
	
	public void moveCursor(int xDiff, int yDiff) {
		Point oldPos = view.getCaretPosition();
		view.setCaretPosition(new Point(oldPos.x + xDiff, oldPos.y + yDiff));
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
		view.repaint();
	}

	/** Scrolls the display down by one line. */
	public void scrollDisplayDown() {
		int removeIndex = getFirstDisplayLine() + firstScrollLineIndex;
		int addIndex = getFirstDisplayLine() + lastScrollLineIndex + 1;
		textLines.remove(removeIndex);
		textLines.add(addIndex, new TextLine());
		view.repaint();
	}
}
