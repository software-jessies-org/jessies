package e.ptextarea;

import java.util.*;

public class PLogicalSegmentIterator implements Iterator<PLineSegment> {
    private PTextArea textArea;
    private int lineIndex;
    private LinkedList<PLineSegment> segmentBuffer = new LinkedList<PLineSegment>();
    
    /**
     * Creates a new PLogicalSegmentIterator which will iterate over the unwrapped segments,
     * starting from the beginning of the segment which contains the given character offset.
     * Note: there is no guarantee that the first segment returned will start at the required char offset!
     */
    public PLogicalSegmentIterator(PTextArea textArea, int offsetContainedByFirstSegment) {
        this.textArea = textArea;
        lineIndex = textArea.getLineOfOffset(offsetContainedByFirstSegment);
        ensureSegmentBufferIsNotEmpty();
        while (segmentBuffer.isEmpty() == false && segmentBuffer.get(0).getEnd() <= offsetContainedByFirstSegment) {
            segmentBuffer.remove(0);
        }
    }
    
    public boolean hasNext() {
        return (segmentBuffer.size() > 0);
    }
    
    private void ensureSegmentBufferIsNotEmpty() {
        while (lineIndex < textArea.getLineCount() && segmentBuffer.size() == 0) {
            for (PLineSegment segment : textArea.getLineSegments(lineIndex)) {
                segmentBuffer.addLast(segment);
            }
            int newlineOffset = textArea.getLineEndOffsetBeforeTerminator(lineIndex);
            if (newlineOffset < textArea.getTextBuffer().length()) {
                segmentBuffer.addLast(new PNewlineSegment(textArea, newlineOffset, newlineOffset + 1, PNewlineSegment.HARD_NEWLINE));
            }
            lineIndex++;
        }
    }
    
    public PLineSegment next() {
        PLineSegment result = segmentBuffer.removeFirst();
        ensureSegmentBufferIsNotEmpty();
        return result;
    }
    
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
