package e.ptextarea;

import java.util.*;

public class PSameStyleSegmentIterator implements Iterator<PLineSegment> {
    private Iterator<PLineSegment> source;
    PLineSegment nextSegment = null;
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
                break;
            }
            nextSegment = null;
        }
        return result;
    }
    
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
