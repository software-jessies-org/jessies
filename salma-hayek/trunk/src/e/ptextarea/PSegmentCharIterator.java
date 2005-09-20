package e.ptextarea;

import java.util.*;

public class PSegmentCharIterator implements PCharIterator {
    private Iterator<PLineSegment> segmentSource;
    private PLineSegment currentSegment;
    private int segmentOffset;
    private boolean iterateForward;
    private int offsetOfLastChar = -1;
    
    public PSegmentCharIterator(Iterator<PLineSegment> segmentSource, int offset, boolean iterateForward) {
        this.segmentSource = segmentSource;
        if (segmentSource.hasNext()) {
            currentSegment = segmentSource.next();
            segmentOffset = offset - currentSegment.getOffset();
        }
        this.iterateForward = iterateForward;
    }
    
    public boolean hasNext() {
        return (currentSegment != null);
    }
    
    private int getIncrement() {
        return iterateForward ? 1 : -1;
    }
    
    public char next() {
        offsetOfLastChar = currentSegment.getOffset() + segmentOffset;
        CharSequence currentSequence = currentSegment.getCharSequence();
        char result = currentSequence.charAt(segmentOffset);
        segmentOffset += getIncrement();
        if (segmentOffset < 0 || segmentOffset >= currentSequence.length()) {
            currentSegment = null;
            if (segmentSource.hasNext()) {
                currentSegment = segmentSource.next();
                segmentOffset = iterateForward ? 0 : currentSegment.getCharSequence().length() - 1;
            }
        }
        return result;
    }
    
    public int getOffsetOfLastChar() {
        return offsetOfLastChar;
    }
}
