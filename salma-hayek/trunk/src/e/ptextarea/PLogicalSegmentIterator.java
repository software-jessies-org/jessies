package e.ptextarea;

public class PLogicalSegmentIterator implements PSegmentIterator {
    private PTextArea textArea;
    private int lineIndex;
    private int segmentInLine;
    private int charOffset;
    private PLineSegment[] lineSegments;
    
    /**
     * Creates a new PLogicalSegmentIterator which will iterate over the unwrapped segments,
     * starting from the beginning of the segment which contains the given character offset.
     * Note: there is no guarantee that the first segment returned will start at the required char offset!
     */
    public PLogicalSegmentIterator(PTextArea textArea, int offsetContainedByFirstSegment) {
        this.textArea = textArea;
        lineIndex = textArea.getLineOfOffset(offsetContainedByFirstSegment);
        lineSegments = textArea.getTextStyler().getLineSegments(lineIndex);
        for (segmentInLine = 0; segmentInLine < lineSegments.length; segmentInLine++) {
            if (lineSegments[segmentInLine].getEnd() > offsetContainedByFirstSegment) {
                break;
            }
        }
        if (segmentInLine == lineSegments.length) {
            this.charOffset = textArea.getLineEndOffsetBeforeTerminator(lineIndex);
        } else {
            this.charOffset = lineSegments[segmentInLine].getOffset();
        }
    }
    
    public boolean hasNext() {
        return (charOffset < textArea.getTextBuffer().length());
    }
    
    public PLineSegment next() {
        PLineSegment result;
        if (segmentInLine > lineSegments.length) {
            lineIndex++;
            lineSegments = textArea.getTextStyler().getLineSegments(lineIndex);
            segmentInLine = 0;
        }  // There is *deliberately* no else here - we wish to fall through to lower processing now.
        
        if (segmentInLine < lineSegments.length) {
            result = lineSegments[segmentInLine++];
        } else if (segmentInLine == lineSegments.length && lineIndex < textArea.getLineCount() - 1) {
            result = new PNewlineSegment(textArea, charOffset, charOffset + 1, PNewlineSegment.HARD_NEWLINE);
            segmentInLine++;
        } else {
            throw new IndexOutOfBoundsException("Went off the end of the text buffer.");
        }
        
        charOffset = result.getEnd();
        return result;
    }
}
