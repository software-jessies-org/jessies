package terminatorn;

/**
A TelnetListener listens to escape sequences and text input coming from the TelnetControl
object.

<p>All methods will be called in the AWT event dispatcher thread.  This is to cut down on
the number of SwingUtilities.invokeLater calls and tiny Runnable objects.

@author Phil Norman
*/

public interface TelnetListener {
	public void processActions(TelnetAction[] actions);

	public void fullReset();
	
	public void setStyle(int style);
	
	public int getStyle();
	
	/**
	* Process the characters in the given line.  The string is composed of normally printable
	* characters.
	*/
	public void processLine(String line);
	
	/** Process a special character, one of CR, LF, TAB or BS. */
	public void processSpecialCharacter(char ch);
	
	/** Saves the current style and location for retrieving later. */
	public void saveCursor();
	
	/** Restores the saved style and location if it was saved earlier. */
	public void restoreCursor();
	
	/** Sets or unsets the use of the alternative buffer. */
	public void useAlternativeBuffer(boolean useAlternativeBuffer);
	
	// VT100 command support.
	
	/** Sets whether the caret should be displayed. */
	public void setCaretDisplay(boolean isDisplayed);
	
	/** Inserts lines at the current caret position. */
	public void insertLines(int count);
	
	/** Deletes characters from the cursor position forward. */
	public void deleteCharacters(int count);
	
	/** Erases from either the start of line or the cursor, to either the end of the line or the cursor. */
	public void killHorizontally(boolean fromStart, boolean toEnd);

	/** Erases from either the top or the cursor line, to either the bottom or the cursor line. */
	public void killVertically(boolean fromTop, boolean toBottom);
	
	/** Sets the position of the cursor to the given x and y coordinates, counted from 1,1 at the top-left corner. */
	public void setCursorPosition(int x, int y);
	
	/** Moves the cursor horizontally by the number of characters in xDiff, negative for left, positive for right. */
	public void moveCursorHorizontally(int xDiff);
	
	/** Moves the cursor vertically by the number of characters in yDiff, negative for up, positive for down. */
	public void moveCursorVertically(int yDiff);

	/** Sets the first and last lines to scroll.  If both are -1, make the entire screen scroll. */
	public void setScrollScreen(int firstLine, int lastLine);

	/** Scrolls the display up by one line. */
	public void scrollDisplayUp();

	/** Scrolls the display down by one line. */
	public void scrollDisplayDown();
	
	/** Changes the window title. */
	public void setWindowTitle(String newWindowTitle);
}
