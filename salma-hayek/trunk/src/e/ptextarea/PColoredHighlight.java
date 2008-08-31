package e.ptextarea;

import java.awt.*;
import java.util.*;

/**
 * A PColoredHighlight is a type of PHighlight which paints itself as a solid block of color on the
 * background.
 * 
 * @author Phil Norman
 */

public abstract class PColoredHighlight extends PHighlight {
    private Color color;
    
    public PColoredHighlight(PTextArea textArea, int startIndex, int endIndex, Color color) {
        super(textArea, startIndex, endIndex);
        this.color = color;
    }
    
    public Color getColor() {
        return color;
    }
    
    public void setColor(Color newColor) {
        this.color = newColor;
    }
    
    protected boolean paintsToEndOfLine() {
        return true;
    }
    
    private int getRightHandLineLimit(int splitLineIndex) {
        if (paintsToEndOfLine()) {
            return textArea.getWidth() - textArea.getInsets().right;
        } else {
            int lineStart = textArea.getSplitLine(splitLineIndex).getTextIndex(textArea);
            Iterator<PLineSegment> it = textArea.getWrappedSegmentIterator(lineStart);
            int result = 0;
            while (it.hasNext()) {
                PLineSegment segment = it.next();
                if (segment.isNewline()) {
                    return result;
                }
                result += segment.getDisplayWidth(textArea.getFontMetrics(textArea.getFont()), result);
            }
            return textArea.getWidth() - textArea.getInsets().right;
        }
    }
    
    @Override
    protected void paintHighlight(Graphics2D g, PCoordinates start, PCoordinates end, Insets insets, int lineHeight, int firstLineIndex, int lastLineIndex) {
        Point startPt = textArea.getViewCoordinates(start);
        Point endPt = textArea.getViewCoordinates(end);
        Color oldColor = g.getColor();
        g.setColor(color);
        int y = textArea.getLineTop(start.getLineIndex());
        for (int i = firstLineIndex; i <= lastLineIndex; ++i) {
            int xStart = (i == start.getLineIndex()) ? startPt.x : insets.left;
            int xEnd = (i == end.getLineIndex()) ? endPt.x : getRightHandLineLimit(i);
            paintRectangleContents(g, new Rectangle(xStart, y, xEnd - xStart, lineHeight));
            y += lineHeight;
        }
        g.setColor(oldColor);
    }
    
    /**
     * Allows subclasses to render a line's highlight however they wish, though they shouldn't draw outside the given rectangle.
     */
    protected void paintRectangleContents(Graphics2D g, Rectangle r) {
        g.fill(r);
    }
}
