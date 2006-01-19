package e.ptextarea;

class SplitLine {
    private PLineList lines;
    private int lineIndex;
    private int offset;
    private int length;
    
    public SplitLine(PLineList lines, int lineIndex, int offset, int length) {
        this.lines = lines;
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
    
    public void setOffset(int offset) {
        this.offset = offset;
    }
    
    public void setLength(int length) {
        this.length = length;
    }
    
    public int getTextIndex() {
        return lines.getLine(lineIndex).getStart() + offset;
    }
    
    public boolean containsIndex(int charIndex) {
        int startIndex = getTextIndex();
        return (charIndex >= startIndex) && (charIndex < startIndex + length);
    }
    
    public CharSequence getContents() {
        CharSequence parent = lines.getLine(lineIndex).getContents();
        int end = offset + length;
        if (length > 0 && parent.charAt(end - 1) == '\n') {
            end -= 1;
        }
        return parent.subSequence(offset, end);
    }
}
