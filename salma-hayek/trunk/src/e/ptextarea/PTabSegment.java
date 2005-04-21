package e.ptextarea;


import java.awt.*;

/**
 * A PTabSegment is a PLineSegment which knows how to draw, and calculate positions of,
 * one or more tab characters.
 * 
 * @author Phil Norman
 */

public class PTabSegment extends PAbstractSegment {
    public static final int MIN_TAB_WIDTH = 5;
    public static final int TAB_WIDTH = 20;
    
    public PTabSegment(PTextArea textArea, int start, int end) {
        super(textArea, start, end, PStyle.NORMAL);
    }
    
    public PLineSegment subSegment(int start, int end) {
        return new PTabSegment(textArea, start + this.start, end + this.start);
    }
    
    public int getDisplayWidth(FontMetrics metrics, int startX) {
        int x = startX + MIN_TAB_WIDTH + TAB_WIDTH * getLength();
        x -= x % TAB_WIDTH;
        return (x - startX);
    }
    
    public int getCharOffset(FontMetrics metrics, int startX, int x) {
        for (int i = 0; i < getLength(); i++) {
            int nextX = startX + MIN_TAB_WIDTH + TAB_WIDTH;
            nextX -= nextX % TAB_WIDTH;
            if (x < nextX) {
                if (x > (nextX + startX) / 2) {
                    return i + 1;
                } else {
                    return i;
                }
            }
            startX = nextX;
        }
        return getLength();
    }
    
    public void paint(Graphics2D graphics, int x, int yBaseline) { }
}
