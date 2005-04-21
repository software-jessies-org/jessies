package e.ptextarea;

import java.awt.*;

public class PNewlineSegment extends PAbstractSegment {
    public static final boolean WRAPPED = false;
    public static final boolean HARD_NEWLINE = true;
    
    private boolean newlineType;
    
    public PNewlineSegment(PTextArea textArea, int start, int end, boolean isHardNewline) {
        super(textArea, start, end, PStyle.NORMAL);
        this.newlineType = isHardNewline;
    }
    
    public PLineSegment subSegment(int start, int end) {
        return new PNewlineSegment(textArea, start + this.start, end + this.start, newlineType);
    }
    
    /** Returns true if this segment represents any line break, be it caused by line wrap or a newline character. */
    public boolean isNewline() {
        return true;
    }
    
    /** Returns true only if this segment represents a hard newline (one representing a newline character). */
    public boolean isHardNewline() {
        return newlineType;
    }
    
    public int getDisplayWidth(FontMetrics metrics, int startX) {
        // Might need to cope with being as wide as the available space on screen if we move dotted
        // line painting in here.
        return 0;
    }
    
    public int getDisplayWidth(FontMetrics metrics, int startX, int charOffset) {
        return subSegment(0, charOffset).getDisplayWidth(metrics, startX);
    }
    
    public int getCharOffset(FontMetrics metrics, int startX, int x) {
        return 0;
    }
    
    public void paint(Graphics2D graphics, int x, int yBaseline) {
        // Consider moving the painting of the dotted line for wrapped text in here if we're a soft newline.
    }
    
    public String toString() {
        return "PNewlineSegment[" + style + ", " + getText() + "]";
    }
}
