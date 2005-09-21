package e.ptextarea;

import java.util.*;

public class PReverseSegmentIterator implements Iterator<PLineSegment> {
    private PTextArea textArea;
    private int lineIndex;
    private LinkedList<PLineSegment> segmentBuffer = new LinkedList<PLineSegment>();
    
    /**
     * Creates a new PReverseSegmentIterator which will iterate backwards over the unwrapped segments,
     * starting from the beginning of the segment which contains the given character offset.
     * Note: there is no guarantee that the first segment returned will start at the required char offset!
     */
    public PReverseSegmentIterator(PTextArea textArea, int offsetContainedByFirstSegment) {
        this.textArea = textArea;
        lineIndex = textArea.getLineOfOffset(offsetContainedByFirstSegment);
        for (PLineSegment segment : textArea.getLineSegments(lineIndex--)) {
            if (segment.getOffset() > offsetContainedByFirstSegment) {
                break;
            }
            // Segments coming later are added to the start of the list, so they'll be returned first.
            segmentBuffer.addFirst(segment);
        }
        ensureSegmentBufferIsNotEmpty();
    }
    
    public boolean hasNext() {
        return (segmentBuffer.size() > 0);
    }
    
    private void ensureSegmentBufferIsNotEmpty() {
        while (lineIndex >= 0 && segmentBuffer.size() == 0) {
            for (PLineSegment segment : textArea.getLineSegments(lineIndex)) {
                segmentBuffer.addFirst(segment);
            }
            int newlineOffset = textArea.getLineEndOffsetBeforeTerminator(lineIndex);
            segmentBuffer.addFirst(new PNewlineSegment(textArea, newlineOffset, newlineOffset + 1, PNewlineSegment.HARD_NEWLINE));
            lineIndex--;
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
