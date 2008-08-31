package e.ptextarea;

final class SplitLine {
    private int lineIndex;
    private final int offset;
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
