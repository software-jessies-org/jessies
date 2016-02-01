package e.ptextarea;

import java.util.*;

/**
 * Filters an Iterator<PLineSegment> skipping any segments with a different style from the first segment returned by the iterator.
 */
public class PSameStyleSegmentIterator implements Iterator<PLineSegment> {
    private Iterator<PLineSegment> source;
    private PLineSegment nextSegment = null;
    private PStyle allowedStyle;
    
    public PSameStyleSegmentIterator(Iterator<PLineSegment> source) {
        this.source = source;
        if (source.hasNext()) {
            nextSegment = source.next();
            allowedStyle = nextSegment.getStyle();
        }
    }
    
    public boolean hasNext() {
        return (nextSegment != null);
    }
    
    public PLineSegment next() {
        PLineSegment result = nextSegment;
        while (source.hasNext()) {
            nextSegment = source.next();
            if (nextSegment.getStyle().equals(allowedStyle)) {
                return result;
            }
        }
        nextSegment = null;
        return result;
    }
    
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
