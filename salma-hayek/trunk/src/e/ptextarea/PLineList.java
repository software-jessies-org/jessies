package e.ptextarea;

import java.util.*;
import e.util.*;

/**
 * A PLineList is an abstraction on top of a PTextBuffer.  This class handles the splitting up of lines,
 * and allows the PTextArea to easily index the text in terms of lines.
 * Note that this class deals only with logical lines, that is lines separated by line terminators.
 * Line wrapping is not handled here.
 * 
 * @author Phil Norman
 */

public class PLineList implements PTextListener {
    private PTextBuffer text;
    private ArrayList<Line> lines;
    private int lastValidLineIndex;
    private ArrayList<PLineListener> listeners = new ArrayList<PLineListener>();
    
    public PLineList(PTextBuffer text) {
        this.text = text;
        text.addTextListener(this);
        generateLines();
    }
    
    public void printLineInfo() {
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            System.err.println(i + ": start " + line.getStart() + ", length " + line.getLength() + ", end " + (line.getStart() + line.getLength()) + ", width " + line.getWidth());
        }
    }
    
    /** Adds a listener which will be informed on line modification, addition and removal. */
    public void addLineListener(PLineListener listener) {
        listeners.add(listener);
    }
    
    /** Removes a listener previously added with the addListener method. */
    public void removeLineListener(PLineListener listener) {
        listeners.remove(listener);
    }
    
    /** Returns the underlying PTextBuffer model. */
    public PTextBuffer getTextBuffer() {
        return text;
    }
    
    /**
     * Returns the character offset into the PTextBuffer corresponding to the line index and char offset
     * contained in the coordinates argument.
     */
    public int getIndex(PCoordinates coords) {
        validateStartPositions(coords.getLineIndex());
        Line line = getLine(coords.getLineIndex());
        return line.getStart() + coords.getCharOffset();
    }
    
    /**
     * Returns the logical coordinates, in terms of line index and character offset, of the given
     * character offset into the PTextBuffer model.
     */
    public PCoordinates getCoordinates(int index) {
        if (index < 0 || index >= text.length()) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds in text of length " + text.length());
        }
        validateStartPositions(lines.size());
        int line = getLineIndex(index);
        int charOffset = index - getLine(line).getStart();
        return new PCoordinates(line, charOffset);
    }
    
    /**
     * Returns the index of the line containing the character with the specified index
     * within the underlying PTextBuffer model.
     */
    public int getLineIndex(int charIndex) {
        int minLine = 0;
        int maxLine = lines.size();
        while (maxLine - minLine > 1) {
            int midLine = (maxLine + minLine) / 2;
            Line mid = getLine(midLine);
            if (mid.containsIndex(charIndex)) {
                return midLine;
            }
            if (charIndex < mid.getStart()) {
                maxLine = midLine;
            } else {
                minLine = midLine;
            }
        }
        return minLine;
    }
    
    /**
     * Returns the number of lines within this model.
     */
    public int size() {
        return lines.size();
    }
    
    /** Returns an object representing information about the line with the given index. */
    public Line getLine(int lineIndex) {
        validateStartPositions(lineIndex);
        return lines.get(lineIndex);
    }
    
    private void fireEvent(PLineEvent event) {
        for (PLineListener listener : listeners) {
            switch (event.getType()) {
            case PLineEvent.ADDED:
                listener.linesAdded(event);
                break;
            case PLineEvent.REMOVED:
                listener.linesRemoved(event);
                break;
            case PLineEvent.CHANGED:
                listener.linesChanged(event);
                break;
            case PLineEvent.COMPLETELY_REPLACED:
                listener.linesCompletelyReplaced(event);
                break;
            }
        }
    }

    /** Handles text insertion notifications from the underlying PTextBuffer model. */
    public void textInserted(PTextEvent event) {
        int lineIndex = getLineIndex(event.getOffset());
        int startIndex = lineIndex;
        CharSequence chars = event.getCharacters();
        int newlineCount = StringUtilities.count(chars, '\n');
        Line line = getLine(lineIndex);
        if (newlineCount > 0) {
            int[] segmentLengths = getLineSegmentLengths(chars, newlineCount);
            int charOffset = event.getOffset() - line.getStart();
            int endChars = line.getLength() - charOffset;  // The characters after the insert position.
            line.setLength(charOffset + segmentLengths[0]);
            for (int i = 1; i < segmentLengths.length; i++) {
                lineIndex++;
                line = new Line(line.getStart() + line.getLength(), segmentLengths[i]);
                lines.add(lineIndex, line);
            }
            line.setLength(line.getLength() + endChars);
        } else {
            line.setLength(line.getLength() + chars.length());
        }
        linesAreInvalidAfter(lineIndex);
        fireEvent(new PLineEvent(this, PLineEvent.CHANGED, startIndex, 1));
        if (newlineCount > 0) {
            fireEvent(new PLineEvent(this, PLineEvent.ADDED, startIndex + 1, newlineCount));
        }
    }
    
    /** Handles text removal notifications from the underlying PTextBuffer model. */
    public void textRemoved(PTextEvent event) {
        int lineIndex = getLineIndex(event.getOffset());
        CharSequence chars = event.getCharacters();
        int newlineCount = StringUtilities.count(chars, '\n');
        Line line = getLine(lineIndex);
        if (newlineCount > 0) {
            int[] segmentLengths = getLineSegmentLengths(chars, newlineCount);
            int charOffset = event.getOffset() - line.getStart();
            for (int i = 2; i < segmentLengths.length; i++) {
                lines.remove(lineIndex + 1);
            }
            int endChars = getLine(lineIndex + 1).getLength() - segmentLengths[segmentLengths.length - 1];
            lines.remove(lineIndex + 1);
            line.setLength(charOffset + endChars);
        } else {
            line.setLength(line.getLength() - chars.length());
        }
        linesAreInvalidAfter(lineIndex);
        fireEvent(new PLineEvent(this, PLineEvent.CHANGED, lineIndex, 1));
        if (newlineCount > 0) {
            fireEvent(new PLineEvent(this, PLineEvent.REMOVED, lineIndex + 1, newlineCount));
        }
    }
    
    private int[] getLineSegmentLengths(CharSequence chars, int newlineCount) {
        int[] result = new int[newlineCount + 1];
        int segment = 0;
        for (int i = 0; i < chars.length(); ++i) {
            result[segment]++;
            if (chars.charAt(i) == '\n') {
                segment++;
            }
        }
        return result;
    }
    
    /** Handles complete text replacement notifications from the underlying PTextBuffer model. */
    public void textCompletelyReplaced(PTextEvent event) {
        generateLines();
        fireEvent(new PLineEvent(this, PLineEvent.COMPLETELY_REPLACED, 0, lines.size()));
    }
    
    private void linesAreInvalidAfter(int lineIndex) {
        lastValidLineIndex = Math.min(lastValidLineIndex, lineIndex);
    }
    
    private void validateStartPositions(int toLineIndex) {
        if (toLineIndex > lastValidLineIndex) {
            for (int i = lastValidLineIndex; i < toLineIndex; i++) {
                // We must get the lines straight out of the ArrayList, and must not call getLine,
                // since that in turn would call this method, resulting in an infinite recursion.
                Line prevLine = lines.get(i);
                Line thisLine = lines.get(i + 1);
                thisLine.setStart(prevLine.getStart() + prevLine.getLength());
            }
            lastValidLineIndex = toLineIndex;
        }
    }
    
    private void generateLines() {
        lines = new ArrayList<Line>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines.add(new Line(start, i + 1 - start));  // The +1 is because newlines stick to previous.
                start = i + 1;
            }
        }
        lines.add(new Line(start, text.length() - start));
        lastValidLineIndex = lines.size() - 1;
    }
    
    /**
     * A PLineList.Line holds information about the location and length of a particular line of
     * text.  It also contains information about how wide this line is when its text is rendered.
     */
    public class Line {
        private int start;
        private int length;
        private int width;
        
        public Line(int start, int length) {
            this.start = start;
            this.length = length;
            setWidthInvalid();
        }
        
        /** Specifies that the render width of the text is now invalid.  To be used only by PTextArea. */
        public void setWidthInvalid() {
            width = -1;
        }
        
        /** Sets the render width of the text.  To be used only by the PTextArea. */
        public void setWidth(int width) {
            this.width = width;
        }
        
        /** Returns whether the render width is valid.  To be used only by the PTextArea. */
        public boolean isWidthValid() {
            return (width != -1);
        }
        
        /** Returns the render width of the text.  To be used only by the PTextArea. */
        public int getWidth() {
            return width;
        }
        
        /** Returns the character offset within the underlying PTextBuffer model of the start of this line. */
        public int getStart() {
            return start;
        }
        
        /** Returns the number of characters in this line, including the newline character if there is one. */
        public int getLength() {
            return length;
        }
        
        private void setStart(int start) {
            this.start = start;
        }
        
        private void setLength(int length) {
            this.length = length;
            setWidthInvalid();
        }
        
        public int getLengthBeforeTerminator() {
            return isLineTerminated() ? length - 1 : length;
        }
        
        /** Returns the offset of the end of this line, not including any newline character. */
        public int getEndOffsetBeforeTerminator() {
            return start + getLengthBeforeTerminator();
        }
        
        /**
         * Returns true if the specified character offset from the start of the underlying PTextBuffer model is
         * held within this line.
         */
        public boolean containsIndex(int charIndex) {
            return (charIndex >= start) && (charIndex < start + length);
        }
        
        /** Returns true when this line is terminated by a newline character. */
        public boolean isLineTerminated() {
            if (length == 0) {
                return false;
            } else {
                return text.charAt(start + length - 1) == '\n';
            }
        }
        
        /**
         * Returns a CharSequence allowing access to the contents of this line, not including
         * any newline characters.
         */
        public CharSequence getContents() {
            return text.subSequence(start, getEndOffsetBeforeTerminator());
        }
    }
}
