package e.ptextarea;

/**
 * Where PLineList represents model "lines" (sequences ending in '\n' or $), a SplitLine is a visible line.
 * A long line (in the PLineList sense) that wraps such that it's displayed on three rows on the screen would have three SplitLine instances, one for each row.
 * 
 * Note that in a file with no wrapped lines, all the SplitLine information is totally superfluous:
 * 
 * Every 'lineIndex' is the SplitLine's index in PTextArea.splitLines.
 * Every 'offset' is 0.
 * Every 'length' is the line length - 1 (because SplitLine's length doesn't include '\n's for some reason).
 */
final class SplitLine {
    // This run's line number in the PLineList.
    private int lineIndex;
    // This run's offset into the PLineList's line.
    private final int offset;
    // This run's number of characters of the PLineList's line.
    private final int length;
    
    public SplitLine(int lineIndex, int offset, int length) {
        this.lineIndex = lineIndex;
        this.offset = offset;
        this.length = length;
    }
    
    public int getLineIndex() {
        return lineIndex;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public int getLength() {
        return length;
    }
    
    public void setLineIndex(int lineIndex) {
        this.lineIndex = lineIndex;
    }
    
    public int getTextIndex(PTextArea textArea) {
        return textArea.getLineList().getLine(lineIndex).getStart() + offset;
    }
    
    public boolean containsIndex(PTextArea textArea, int charIndex) {
        int startIndex = getTextIndex(textArea);
        return (charIndex >= startIndex) && (charIndex < startIndex + length);
    }
    
    public CharSequence getContents(PTextArea textArea) {
        CharSequence parent = textArea.getLineList().getLineContents(lineIndex);
        int end = offset + length;
        if (length > 0 && parent.charAt(end - 1) == '\n') {
            end -= 1;
        }
        return parent.subSequence(offset, end);
    }
}
