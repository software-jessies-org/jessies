package e.ptextarea;


import java.awt.*;

/**
 * A PTabSegment is a PLineSegment which knows how to draw, and calculate positions of,
 * one or more tab characters.
 * 
 * @author Phil Norman
 */

public class PTabSegment implements PLineSegment {
    public static final int MIN_TAB_WIDTH = 5;
    public static final int TAB_WIDTH = 20;
    
    private String text;
    
    public PTabSegment(String text) {
        this.text = text;
    }
    
    public boolean isVisible() {
        return false;
    }
    
    public int getStyleIndex() {
        return 0;
    }
    
    public String getText() {
        return text;
    }
    
    public int getLength() {
        return text.length();
    }
    
    public int getDisplayWidth(FontMetrics metrics, int startX) {
        int x = startX + MIN_TAB_WIDTH + TAB_WIDTH * text.length();
        x -= x % TAB_WIDTH;
        return (x - startX);
    }
    
    public int getDisplayWidth(FontMetrics metrics, int startX, int charOffset) {
        return (new PTabSegment(text.substring(0, charOffset))).getDisplayWidth(metrics, startX);
    }
    
    public int getCharOffset(FontMetrics metrics, int startX, int x) {
        for (int i = 0; i < text.length(); i++) {
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
        return text.length();
    }
}
