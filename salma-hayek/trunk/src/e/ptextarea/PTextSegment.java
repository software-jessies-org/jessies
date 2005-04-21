package e.ptextarea;


import java.awt.*;

/**
 * A PTextSegment is a PLineSegment which knows how to deal with styled characters.
 * 
 * @author Phil Norman
 */

public class PTextSegment extends PAbstractSegment {
    
    public PTextSegment(PTextArea textArea, int start, int end, PStyle style) {
        super(textArea, start, end, style);
    }
    
    public PLineSegment subSegment(int start, int end) {
        return new PTextSegment(textArea, start + this.start, end + this.start, style);
    }
    
    public int getDisplayWidth(FontMetrics metrics, int startX) {
        return metrics.stringWidth(getText());
    }
    
    public int getCharOffset(FontMetrics metrics, int startX, int x) {
        char[] ch = getText().toCharArray();
        int min = 0;
        int max = ch.length;
        while (max - min > 1) {
            int mid = (min + max) / 2;
            int width = metrics.charsWidth(ch, 0, mid);
            if (width > x - startX) {
                max = mid;
            } else {
                min = mid;
            }
        }
        int charPixelOffset = x - startX - metrics.charsWidth(ch, 0, min);
        if (charPixelOffset > metrics.charWidth(ch[min]) / 2) {
            min++;
        }
        return min;
    }
    
    public void paint(Graphics2D graphics, int x, int yBaseline) {
        graphics.drawString(getText(), x, yBaseline);
    }
    
    public String toString() {
        return "PTextSegment[" + style + ", " + getText() + "]";
    }
}
