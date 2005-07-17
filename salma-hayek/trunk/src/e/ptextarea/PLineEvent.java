package e.ptextarea;


public class PLineEvent {
    public static final int CHANGED = 1;
    public static final int ADDED = 2;
    public static final int REMOVED = 3;
    
    private PLineList lines;
    private int type;
    private int lineIndex;
    private int length;
    
    public PLineEvent(PLineList lines, int type, int lineIndex, int length) {
        this.lines = lines;
        this.type = type;
        this.lineIndex = lineIndex;
        this.length = length;
    }
    
    public PLineList getLineList() {
        return lines;
    }
    
    public int getType() {
        return type;
    }
    
    public int getLineIndex() {
        return lineIndex;
    }
    
    public int getLength() {
        return length;
    }
}
