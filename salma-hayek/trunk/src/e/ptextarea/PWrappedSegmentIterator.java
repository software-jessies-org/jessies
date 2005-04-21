package e.ptextarea;

public class PWrappedSegmentIterator implements PSegmentIterator {
    private PTextArea textArea;
    private int charOffset;
    private PSegmentIterator logicalIterator;
    private PLineSegment currentSegment;
    private PTextArea.SplitLine currentLine;
    private boolean sawNewlineSegment = false;
    
    public PWrappedSegmentIterator(PTextArea textArea, int charOffset) {
        this.textArea = textArea;
        this.charOffset = charOffset;
        logicalIterator = textArea.getLogicalSegmentIterator(charOffset);
        currentLine = textArea.getSplitLineOfOffset(charOffset);
    }
    
    public boolean hasNext() {
        return (charOffset < textArea.getPTextBuffer().length());
    }
    
    public PLineSegment next() {
        PLineSegment result = null;
        if (charOffset == currentLine.getTextIndex() + currentLine.getLength()) {
            currentLine = textArea.getSplitLineOfOffset(charOffset);
            if (sawNewlineSegment == false) {
                result = new PNewlineSegment(textArea, charOffset, charOffset, PNewlineSegment.WRAPPED);
            }
        }
        if (result == null) {
            if (currentSegment == null) {
                currentSegment = logicalIterator.next();
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
        sawNewlineSegment = result.isHardNewline();
        charOffset = result.getEnd();
        return result;
    }
}
