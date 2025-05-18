package e.ptextarea;


public class PTextEvent {
    public static final int INSERT = 1;
    public static final int REMOVE = 2;
    public static final int COMPLETE_REPLACEMENT = 3;
    
    private PTextBuffer pText;
    private int eventType;
    private int offset;
    private CharSequence characters;

    public PTextEvent(PTextBuffer pText, int eventType, int offset, CharSequence characters) {
        this.pText = pText;
        this.eventType = eventType;
        this.offset = offset;
        this.characters = characters;
    }
    
    public PTextBuffer getTextBuffer() {
        return pText;
    }
    
    public String toString() {
        if (isCompleteReplacement()) {
            return "PTextEvent[COMPLETE_REPLACEMENT]";
        }
        if (isInsert()) {
            return "PTextEvent[@" + offset + " add \"" + characters + "\"]";
        }
        return "PTextEvent[@" + offset + " del \"" + characters + "\"]";
    }
    
    public boolean isInsert() {
        return (eventType == INSERT);
    }
    
    public boolean isRemove() {
        return (eventType == REMOVE);
    }
    
    public boolean isCompleteReplacement() {
        return (eventType == COMPLETE_REPLACEMENT);
    }
    
    public int getEventType() {
        return eventType;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public int getLength() {
        return characters.length();
    }
    
    public CharSequence getCharacters() {
        return characters;
    }
}
