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
            return textArea.getWidth();
        } else {
            int lineStart = textArea.getSplitLine(splitLineIndex).getTextIndex();
            Iterator<PLineSegment> it = textArea.getWrappedSegmentIterator(lineStart);
            int result = 0;
            while (it.hasNext()) {
                PLineSegment segment = it.next();
                if (segment.isNewline()) {
                    return result;
                }
                result += segment.getDisplayWidth(textArea.getFontMetrics(textArea.getFont()), result);
            }
            return textArea.getWidth();
        }
    }
    
    public void paint(Graphics2D graphics, PCoordinates start, PCoordinates end) {
        Point startPt = textArea.getViewCoordinates(start);
        Point endPt = textArea.getViewCoordinates(end);
        Color oldColor = graphics.getColor();
        graphics.setColor(color);
        int y = textArea.getLineTop(start.getLineIndex());
        int lineHeight = textArea.getLineHeight();
        for (int i = start.getLineIndex(); i <= end.getLineIndex(); i++) {
            int xStart = (i == start.getLineIndex()) ? startPt.x : 0;
            int xEnd = (i == end.getLineIndex()) ? endPt.x : getRightHandLineLimit(i);
            paintRectangleContents(graphics, new Rectangle(xStart, y, xEnd - xStart, lineHeight));
            y += lineHeight;
        }
        graphics.setColor(oldColor);
    }
    
    public void paintRectangleContents(Graphics2D graphics, Rectangle rectangle) {
        graphics.fill(rectangle);
    }
}
