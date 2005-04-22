package e.ptextarea;

public class PWrappedSegmentIterator implements PSegmentIterator {
    private PTextArea textArea;
    private int charOffset;
    private int nextSplitLineIndex;
    private CachedSegmentIterator logicalIterator;
    private PLineSegment currentSegment;
    private PTextArea.SplitLine currentLine;    
    public PWrappedSegmentIterator(PTextArea textArea, int charOffset) {
        this.textArea = textArea;
        this.charOffset = charOffset;
        logicalIterator = new CachedSegmentIterator(textArea.getLogicalSegmentIterator(charOffset));
        nextSplitLineIndex = textArea.getCoordinates(charOffset).getLineIndex();
        currentLine = textArea.getSplitLine(nextSplitLineIndex++);
    }
    
    public boolean hasNext() {
        return (charOffset < textArea.getTextBuffer().length());
    }
    
    public PLineSegment next() {
        PLineSegment result = null;
        if (charOffset == currentLine.getTextIndex() + currentLine.getLength()) {
            currentLine = textArea.getSplitLine(nextSplitLineIndex++);
            if ((currentSegment == null) && logicalIterator.peekNext().isNewline()) {
                result = logicalIterator.next();
            } else {
                result = new PNewlineSegment(textArea, charOffset, charOffset, PNewlineSegment.WRAPPED);
            }
        }
        if (result == null) {
            if (currentSegment == null) {
                currentSegment = logicalIterator.next();
                if (currentSegment.getOffset() < charOffset) {
                    currentSegment = currentSegment.subSegment(charOffset - currentSegment.getOffset());
                }
            }
            result = currentSegment;
            int lineEndOffset = currentLine.getTextIndex() + currentLine.getLength();
            if (lineEndOffset < currentSegment.getEnd()) {
                int splitOffset = lineEndOffset - currentSegment.getOffset();
                result = currentSegment.subSegment(0, splitOffset);
                currentSegment = currentSegment.subSegment(splitOffset);
            } else {
                currentSegment = null;
            }
        }
        charOffset = result.getEnd();
        return result;
    }
    
    private class CachedSegmentIterator implements PSegmentIterator {
        private PSegmentIterator source;
        private PLineSegment next = null;
        
        private CachedSegmentIterator(PSegmentIterator source) {
            this.source = source;
        }
        
        public PLineSegment peekNext() {
            if (next == null) {
                next = source.next();
            }
            return next;
        }
        
        public PLineSegment next() {
            PLineSegment result = (next != null) ? next : source.next();
            next = null;
            return result;
        }
        
        public boolean hasNext() {
            return (next != null) || source.hasNext();
        }
    }
}
