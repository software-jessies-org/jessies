package e.ptextarea;

import java.awt.*;

public abstract class PAbstractSegment implements PLineSegment {
    protected PTextArea textArea;
    protected int start;
    protected int end;
    protected PStyle style;
    
    public PAbstractSegment(PTextArea textArea, int start, int end, PStyle style) {
        this.textArea = textArea;
        this.start = start;
        this.end = end;
        this.style = style;
    }
    
    public PStyle getStyle() {
        return style;
    }
    
    public CharSequence getCharSequence() {
        return textArea.getTextBuffer().subSequence(start, end);
    }
    
    public String getViewText() {
        return getCharSequence().toString();
    }
    
    public PLineSegment subSegment(int start) {
        return subSegment(start, getModelTextLength());
    }
    
    public abstract PLineSegment subSegment(int start, int end);
    
    public int getOffset() {
        return start;
    }
    
    public int getModelTextLength() {
        return end - start;
    }
    
    public int getEnd() {
        return end;
    }
    
    /** Returns true if this segment represents any line break, be it caused by line wrap or a newline character. */
    public boolean isNewline() {
        return false;
    }
    
    /** Returns true only if this segment represents a hard newline (one representing a newline character). */
    public boolean isHardNewline() {
        return false;
    }
    
    protected FontMetrics getFontMetrics() {
        return textArea.getFontMetrics(style.getFontFlags());
    }
    
    public int getDisplayWidth(int startX) {
        return getFontMetrics().stringWidth(getViewText());
    }
    
    public int getDisplayWidth(int startX, int charOffset) {
        return subSegment(0, charOffset).getDisplayWidth(startX);
    }
    
    public abstract int getCharOffset(int startX, int x);
    
    public abstract void paint(Graphics2D g, int x, int yBaseline);
    
    public String toString() {
        return "PAbstractSegment[" + style + ",start=" + start + ",end=" + end + ",\"" + getViewText() + "\"]";
    }
}
