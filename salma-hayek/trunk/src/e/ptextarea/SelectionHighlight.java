package e.ptextarea;

import java.awt.*;

/**
 * Highlights the selection. This is fancier than most highlights; it has a
 * slightly darker border drawn around it, and it changes color depending on
 * whether the text area has the focus.
 */
public class SelectionHighlight extends PHighlight {
    private static final Color FOCUSED_SELECTION_COLOR = new Color(0.70f, 0.83f, 1.00f, 0.5f);
    private static final Color FOCUSED_SELECTION_BOUNDARY_COLOR = new Color(0.5f, 0.55f, 0.7f, 0.75f);
    private static final Color UNFOCUSED_SELECTION_COLOR = new Color(0.83f, 0.83f, 0.83f, 0.5f);
    
    private int id;
    private static int nextId = 0;
    
    public SelectionHighlight(PTextArea textArea, int startIndex, int endIndex) {
        super(textArea, startIndex, endIndex);
        id = nextId++;
    }
    
    public boolean isEmpty() {
        return (getStartIndex() == getEndIndex());
    }
    
    @Override
    protected void paintHighlight(Graphics2D graphics, PCoordinates start, PCoordinates end, Insets insets, int lineHeight, int firstLineIndex, int lastLineIndex) {
        if (isEmpty()) {
            return;
        }
        
        Point startPt = textArea.getViewCoordinates(start);
        Point endPt = textArea.getViewCoordinates(end);
        Color oldColor = graphics.getColor();
        int y = textArea.getLineTop(firstLineIndex);
        for (int i = firstLineIndex; i <= lastLineIndex; ++i) {
            int xStart = (i == start.getLineIndex()) ? startPt.x : insets.left;
            int xEnd = (i == end.getLineIndex()) ? endPt.x : textArea.getWidth() - insets.right;
            if (xStart == xEnd) {
                // If there's nothing to highlight on a line, avoid painting
                // the border. If it's painted, it'll be visible as a single
                // pixel wide, one line high tail at the bottom left corner of
                // the highlight. Arguably this is useful because it shows that
                // the newline is part of the selection, but no other text
                // component highlights like this.
                continue;
            }
            graphics.setColor(textArea.isFocusOwner() ? FOCUSED_SELECTION_COLOR : UNFOCUSED_SELECTION_COLOR);
            graphics.fill(new Rectangle(xStart, y, xEnd - xStart, lineHeight));
            int yBottom = y + lineHeight - 1;
            if (textArea.isFocusOwner()) {
                graphics.setColor(FOCUSED_SELECTION_BOUNDARY_COLOR);
                // Draw this shape at the top of the selection (C will be
                // zero-length for a single-line selection):
                //        _____________________
                // _______|B         A
                //    C
                if (i == start.getLineIndex()) {
                    if (xStart > 0) {
                        // B
                        graphics.drawLine(xStart, y, xStart, yBottom);
                    }
                    // A
                    graphics.drawLine(xStart, y, xEnd, y);
                } else if (i == start.getLineIndex() + 1) {
                    final int Bx = Math.min(xEnd, startPt.x);
                    if (Bx > insets.left) {
                        // C
                        graphics.drawLine(insets.left, y, Bx, y);
                    }
                }
                
                // Draw this shape at the bottom of the selection (E will
                // be zero-length for a single-line selection):
                //            _________________
                // ___________|D     E
                //      F
                if (i == end.getLineIndex()) {
                    if (xEnd < textArea.getWidth() - insets.right) {
                        // D
                        graphics.drawLine(xEnd, y, xEnd, yBottom);
                    }
                    // F
                    graphics.drawLine(xStart, yBottom, xEnd, yBottom);
                } else if (i == end.getLineIndex() - 1) {
                    // E
                    graphics.drawLine(Math.max(endPt.x, xStart), yBottom, xEnd, yBottom);
                }
                
                // Draw vertical lines at the left and right edges of the
                // selection; we only really need these if we have non-zero
                // margins, but they're important then.
                graphics.drawLine(xStart, y, xStart, yBottom);
                graphics.drawLine(xEnd, y, xEnd, yBottom);
            }
            y += lineHeight;
        }
        graphics.setColor(oldColor);
    }
    
    @Override
    public String toString() {
        return "SelectionHighlight[" + id + ": " + getStartIndex() + ", " + getEndIndex() + "]";
    }
    
    @Override
    public String getHighlighterName() {
        return "Selection";
    }
}
