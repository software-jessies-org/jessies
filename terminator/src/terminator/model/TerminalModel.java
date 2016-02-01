package terminator.model;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import e.util.*;
import terminator.terminal.*;
import terminator.view.*;
import terminator.Terminator;

public class TerminalModel {
    private TerminalView view;
    private int width;
    private int height;
    private ArrayList<TextLine> textLines = new ArrayList<TextLine>();
    private Style currentStyle = Style.getDefaultStyle();
    private int firstScrollLineIndex;
    private int lastScrollLineIndex;
    private Location cursorPosition;
    private int lastValidStartIndex = 0;
    private boolean insertMode = false;
    private ArrayList<Integer> tabPositions = new ArrayList<Integer>();
    private int maxLineWidth = width;
    private int flushes = 0;
    
    // Used for reducing the number of lines changed events sent up to the view.
    private int firstLineChanged;
    
    // Fields used for saving and restoring state.
    private Location savedPosition;
    private Style savedStyle;
    
    // Fields used for saving and restoring the 'real' screen while the alternate buffer is in use.
    private TextLine[] savedScreen;
    
    public TerminalModel(TerminalView view, int width, int height) {
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
            int thisStartLineIndex = textLines.get(lineNumber).getLineStartIndex();
            if (thisStartLineIndex <= highestStartLineIndex) {
                throw new RuntimeException("the lineStartIndex must increase monotonically as the line number increases");
            }
            highestStartLineIndex = thisStartLineIndex;
        }
    }
    
    public void clearScrollBuffer() {
        // FIXME: really, we should still clear everything off-screen.
        if (usingAlternateBuffer()) {
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
        ArrayList<TextLine> retainedLines = new ArrayList<TextLine>(textLines.subList(cursorPosition.getLineIndex(), textLines.size()));
        
        // Revert to just the right number of empty lines to fill the
        // current window size.
        // Using a new ArrayList ensures we free space without risking
        // expensive nulling-out of now-unused elements. The assumption
        // being that we're most likely to be asked to clear the
        // scrollback when it's insanely large.
        textLines = new ArrayList<TextLine>();
        setSize(width, view.getVisibleSizeInCharacters().height);
        maxLineWidth = width;
        
        // Re-insert the lines after the cursor.
        for (int i = 0; i < retainedLines.size(); ++i) {
            insertLine(i, retainedLines.get(i));
        }
        
        // Make sure all the lines will be redrawn.
        view.sizeChanged();
        lineIsDirty(0);
        
        resetCursorPosition();
        
        // Redraw ourselves.
        view.repaint();
        checkInvariant();
    }
    
    private void resetCursorPosition() {
        // Re-position the cursor.
        // FIXME: it's a bit crazy that these aren't tied!
        // FIXME: it's even crazier that they use different origins!
        setCursorPosition(-1, 1);
        view.setCursorPosition(cursorPosition);
    }
    
    public void flushScrollBuffer() {
        clearScrollBuffer();
        ++ flushes;
    }
    
    public int getFlushes() {
        return flushes;
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
    
    /** Sets or unsets the use of the alternate buffer. */
    public void useAlternateBuffer(boolean useAlternateBuffer) {
        if (useAlternateBuffer == usingAlternateBuffer()) {
            return;
        }
        // Since we don't save the alternate buffer, reset background to default before switching to it.
        // When switching back, we don't want the background that was probably set while in the alternate buffer.
        // FIXME: should we save the screen's old background along with savedScreen?
        view.setBackground(Terminator.getPreferences().getColor("background"));
        if (useAlternateBuffer) {
            savedScreen = new TextLine[height];
            for (int i = 0; i < height; i++) {
                int lineIndex = getFirstDisplayLine() + i;
                savedScreen[i] = getTextLine(lineIndex);
                textLines.set(lineIndex, new TextLine(view.getBackground()));
            }
        } else {
            for (int i = 0; i < height; i++) {
                int lineIndex = getFirstDisplayLine() + i;
                textLines.set(lineIndex, i >= savedScreen.length ? new TextLine(view.getBackground()) : savedScreen[i]);
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
    
    /** Returns true when the alternate buffer is in use. */
    public boolean usingAlternateBuffer() {
        return (savedScreen != null);
    }
    
    public void setTabAtCursor() {
        int newPos = cursorPosition.getCharOffset();
        for (int i = 0; i < tabPositions.size(); i++) {
            int pos = tabPositions.get(i);
            if (pos == newPos) {
                return;
            } else if (pos > newPos) {
                tabPositions.add(i, newPos);
                return;
            }
        }
        tabPositions.add(newPos);
    }
    
    public void removeTabAtCursor() {
        tabPositions.remove(cursorPosition.getCharOffset());
    }
    
    public void removeAllTabs() {
        tabPositions.clear();
    }
    
    private int getNextTabPosition(int charOffset) {
        for (int i = 0; i < tabPositions.size(); i++) {
            int pos = tabPositions.get(i);
            if (pos > charOffset) {
                return pos;
            }
        }
        // No special tab to our right; return the default 8-separated tab stop.
        return (charOffset + 8) & ~7;
    }
    
    /** Returns the length of the indexed line including the terminating NL. */
    public int getLineLength(int lineIndex) {
        return getTextLine(lineIndex).length() + 1;
    }
    
    /** Returns the start character index of the indexed line. */
    public int getStartIndex(int lineIndex) {
        ensureValidStartIndex(lineIndex);
        return getTextLine(lineIndex).getLineStartIndex();
    }
    
    /**
     * Returns a Location describing the line and offset at which the given char index exists.
     * If the index is actually larger than the screen area, returns a 'fake' location to the right
     * of the end of the last line.
     */
    public Location getLocationFromCharIndex(int charIndex) {
        int lowLine = 0;
        int highLine = textLines.size();
        
        while (highLine - lowLine > 1) {
            int midLine = (lowLine + highLine) / 2;
            int mid = getStartIndex(midLine);
            if (mid <= charIndex) {
                lowLine = midLine;
            } else {
                highLine = midLine;
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
    
    private void lineIsDirty(int dirtyLineIndex) {
        lastValidStartIndex = Math.min(lastValidStartIndex, dirtyLineIndex + 1);
    }
    
    private void ensureValidStartIndex(int lineIndex) {
        if (lineIndex > lastValidStartIndex) {
            for (int i = lastValidStartIndex; i < lineIndex; i++) {
                TextLine line = getTextLine(i);
                getTextLine(i + 1).setLineStartIndex(line.getLineStartIndex() + line.lengthIncludingNewline());
            }
            lastValidStartIndex = lineIndex;
        }
    }
    
    public int getLineCount() {
        return textLines.size();
    }
    
    public void fullReset() {
        resetCursorPosition();
        int firstLineToClear = getFirstDisplayLine();
        for (int i = 0; i < height; i++) {
            getTextLine(firstLineToClear + i).clear();
        }
        view.repaint();
    }
    
    public void linesChangedFrom(int firstLineChanged) {
        this.firstLineChanged = Math.min(this.firstLineChanged, firstLineChanged);
        view.getSelectionHighlighter().textChanged(new Location(firstLineChanged, 0), new Location(getLineCount() + 1, 0));
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
        for (TerminalAction action : actions) {
            action.perform(this);
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
    
    public void setStyle(Style style) {
        this.currentStyle = style;
    }
    
    public Style getStyle() {
        return currentStyle;
    }
    
    public void moveToLine(int index) {
        if (index > getFirstDisplayLine() + lastScrollLineIndex) {
            insertLine(index);
        } else {
            cursorPosition = new Location(index, cursorPosition.getCharOffset());
        }
    }
    
    public void insertLine(int index) {
        insertLine(index, new TextLine(view.getBackground()));
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
            if (usingAlternateBuffer() || (firstScrollLineIndex > 0)) {
                // If the program has defined scroll bounds, newline-adding actually chucks away
                // the first scroll line, rather than just scrolling everything upwards like we normally
                // do.  This makes vim work better.  Also, if we're using the alternate buffer, we
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
    
    public TextLine getTextLine(int index) {
        if (index >= textLines.size()) {
            Log.warn("TextLine requested for index " + index + ", size of buffer is " + textLines.size() + ".", new Exception("stack trace"));
            return new TextLine(view.getBackground());
        }
        return textLines.get(index);
    }
    
    public void setSize(int width, int height) {
        this.width = width;
        lineIsDirty(0);
        if (this.height > height && textLines.size() >= this.height) {
            for (int i = 0; i < (this.height - height); i++) {
                int lineToRemove = textLines.size() - 1;
                if (usingAlternateBuffer() || (getTextLine(lineToRemove).length() == 0 && cursorPosition.getLineIndex() != lineToRemove)) {
                    textLines.remove(lineToRemove);
                }
            }
        } else if (this.height < height) {
            for (int i = 0; i < (height - this.height); i++) {
                if (usingAlternateBuffer() || getFirstDisplayLine() <= 0) {
                    textLines.add(new TextLine(view.getBackground()));
                }
            }
        }
        this.height = height;
        firstScrollLineIndex = 0;
        lastScrollLineIndex = height - 1;
        while (getFirstDisplayLine() < 0) {
            textLines.add(new TextLine(view.getBackground()));
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
        TextLine textLine = getTextLine(cursorPosition.getLineIndex());
        if (insertMode) {
            //Log.warn("Inserting text \"" + line + "\" at " + cursorPosition + ".");
            textLine.insertTextAt(cursorPosition.getCharOffset(), line, currentStyle);
        } else {
            //Log.warn("Writing text \"" + line + "\" at " + cursorPosition + ".");
            textLine.writeTextAt(cursorPosition.getCharOffset(), line, currentStyle);
        }
        textAdded(line.length());
    }
    
    private void textAdded(int length) {
        TextLine textLine = getTextLine(cursorPosition.getLineIndex());
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
        TextLine textLine = getTextLine(cursorPosition.getLineIndex());
        int startOffset = cursorPosition.getCharOffset();
        int tabLength = nextTabLocation - startOffset;
        // We want to insert our special tabbing characters (see getTabString) when inserting a tab or outputting one at the end of a line, so that text copied from the output of (say) cat(1) will be pasted with tabs preserved.
        boolean endOfLine = (startOffset == textLine.length());
        if (insertMode || endOfLine) {
            textLine.insertTabAt(startOffset, tabLength, currentStyle);
        } else {
            // Emacs, source of all bloat, uses \t\b\t sequences around tab stops (in lines with no \t characters) if you hold down right arrow. The call to textAdded below moves the cursor, which is all we're supposed to do.
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
        TextLine line = getTextLine(cursorPosition.getLineIndex());
        int start = cursorPosition.getCharOffset();
        int end = start + count;
        line.killText(start, end);
        lineIsDirty(cursorPosition.getLineIndex() + 1);  // cursorPosition.y's line still has a valid *start* index.
        linesChangedFrom(cursorPosition.getLineIndex());
    }
    
    public void killHorizontally(boolean fromStart, boolean toEnd) {
        if (!fromStart && !toEnd) {
            throw new IllegalArgumentException("fromStart=" + fromStart + " toEnd=" + toEnd);
        }
        TextLine line = getTextLine(cursorPosition.getLineIndex());
        int oldLineLength = line.length();
        if (!toEnd) {
            // If clearing before current position, we have to leave spaces.
            // The current position is included in the deletion, hence + 1.
            line.writeTextAt(0, StringUtilities.nCopies(cursorPosition.getCharOffset() + 1, ' '), currentStyle);
        } else {
            line.setBackground(currentStyle.getBackground());
            int start = fromStart ? 0 : cursorPosition.getCharOffset();
            line.killText(start, oldLineLength);
        }
        lineIsDirty(cursorPosition.getLineIndex() + 1);  // cursorPosition.y's line still has a valid *start* index.
        linesChangedFrom(cursorPosition.getLineIndex());
    }
    
    /** Erases from either the top or the cursor, to either the bottom or the cursor. */
    public void eraseInPage(boolean fromTop, boolean toBottom) {
        if (!fromTop && !toBottom) {
            throw new IllegalArgumentException("fromTop=" + fromTop + " toBottom=" + toBottom);
        }
        // Should produce "hi\nwo":
        // echo $'\n\n\nworld\x1b[A\rhi\x1b[B\x1b[J'
        // Should produce "   ld":
        // echo $'\n\n\nworld\x1b[A\rhi\x1b[B\x1b[1J'
        // Should clear the screen:
        // echo $'\n\n\nworld\x1b[A\rhi\x1b[B\x1b[2J'
        int start = fromTop ? getFirstDisplayLine() : cursorPosition.getLineIndex();
        int startClearing = fromTop ? start : start + 1;
        int endClearing = toBottom ? getLineCount() : cursorPosition.getLineIndex();
        Color currentBackground = currentStyle.getBackground();
        for (int i = startClearing; i < endClearing; i++) {
            TextLine cl = getTextLine(i);
            cl.clear();
            cl.setBackground(currentBackground);
        }
        TextLine line = getTextLine(cursorPosition.getLineIndex());
        int oldLineLength = line.length();
        if (toBottom) {
            view.setBackground(currentBackground);
            line.killText(cursorPosition.getCharOffset(), oldLineLength);
        } else /* only fromTop = true */ {
            // The current position is always erased, hence the + 1.
            line.writeTextAt(0, StringUtilities.nCopies(cursorPosition.getCharOffset() + 1, ' '), currentStyle);
        }
        lineIsDirty(start + 1);
        linesChangedFrom(start);
    }
    
    /**
     * Sets the position of the cursor to the given x and y coordinates, counted from 1,1 at the top-left corner.
     * If either x or y is -1, that coordinate is left unchanged.
     */
    public void setCursorPosition(int x, int y) {
        // Although the cursor positions are supposed to be measured
        // from (1,1), there's nothing to stop a badly-behaved program
        // from sending (0,0). ASUS routers do this (they're rubbish).
        
        int charOffset = cursorPosition.getCharOffset();
        if (x != -1) {
            // Translate from 1-based coordinates to 0-based.
            charOffset = Math.max(0, x - 1);
            charOffset = Math.min(charOffset, width - 1);
        }
        
        int lineIndex = cursorPosition.getLineIndex();
        if (y != -1) {
            // Translate from 1-based coordinates to 0-based.
            int lineOffsetFromStartOfDisplay = Math.max(0, y - 1);
            lineOffsetFromStartOfDisplay = Math.min(lineOffsetFromStartOfDisplay, height - 1);
            // Although the escape sequence was in terms of a line on the display, we need to take the lines above the display into account.
            lineIndex = getFirstDisplayLine() + lineOffsetFromStartOfDisplay;
        }
        
        cursorPosition = new Location(lineIndex, charOffset);
    }
    
    /** Moves the cursor horizontally by the number of characters in xDiff, negative for left, positive for right. */
    public void moveCursorHorizontally(int xDiff) {
        int charOffset = cursorPosition.getCharOffset() + xDiff;
        int lineIndex = cursorPosition.getLineIndex();
        // Test cases:
        // /bin/echo -e 'hello\n\bhello'
        // /bin/echo -e 'hello\n\033[1Dhello'
        if (charOffset < 0) {
            charOffset = 0;
        }
        // Constraining charOffset here stops line editing working properly on Titan serial consoles.
        //charOffset = Math.min(charOffset, width - 1);
        cursorPosition = new Location(lineIndex, charOffset);
    }
    
    /** Moves the cursor vertically by the number of characters in yDiff, negative for up, positive for down. */
    public void moveCursorVertically(int yDiff) {
        int y = cursorPosition.getLineIndex() + yDiff;
        y = Math.max(getFirstDisplayLine(), y);
        y = Math.min(y, textLines.size() - 1);
        cursorPosition = new Location(y, cursorPosition.getCharOffset());
    }
    
    /** Sets the first and last lines to scroll.  If both are -1, make the entire screen scroll. */
    public void setScrollingRegion(int firstLine, int lastLine) {
        firstScrollLineIndex = ((firstLine == -1) ? 1 : firstLine) - 1;
        lastScrollLineIndex = ((lastLine == -1) ? height : lastLine) - 1;
    }
    
    /** Scrolls the display up by one line. */
    public void scrollDisplayUp() {
        int addIndex = getFirstDisplayLine() + firstScrollLineIndex;
        int removeIndex = getFirstDisplayLine() + lastScrollLineIndex + 1;
        textLines.add(addIndex, new TextLine(view.getBackground()));
        textLines.remove(removeIndex);
        lineIsDirty(addIndex);
        linesChangedFrom(addIndex);
        view.repaint();
        checkInvariant();
    }
    
    /** Delete one line, moving everything below up and inserting a blank line at the bottom. */
    public void deleteLine() {
        int removeIndex = cursorPosition.getLineIndex();
        int addIndex = getFirstDisplayLine() + lastScrollLineIndex + 1;
        textLines.add(addIndex, new TextLine(view.getBackground()));
        textLines.remove(removeIndex);
        lineIsDirty(removeIndex);
        linesChangedFrom(removeIndex);
        view.repaint();
        checkInvariant();
    }
    
    public void setWindowTitle(String newWindowTitle) {
        JTerminalPane terminalPane = (JTerminalPane) SwingUtilities.getAncestorOfClass(JTerminalPane.class, view);
        terminalPane.setTerminalName(newWindowTitle);
    }
}
