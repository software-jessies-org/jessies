package e.ptextarea;


import java.awt.*;

/**
 * A PHighlight is an anchored region of text which is specially painted by PTextArea.
 * The PHighlight is automatically destroyed when either of its start and end points is
 * destroyed.  Painting is performed by implementing the abstract paint method.
 *
 * @author Phil Norman
 */

public abstract class PHighlight extends PAnchorRegion {
    protected PTextArea textArea;
    
    public PHighlight(PTextArea textArea, int startIndex, int endIndex) {
        super(textArea.getAnchorSet(), startIndex, endIndex);
        this.textArea = textArea;
    }
    
    /**
     * Removes this highlight from the PTextArea.  This method is called when one or other
     * of the end points of the highlight is destroyed.
     */
    public void delete() {
        textArea.removeHighlight(this);
    }
    
    public void paint(Graphics2D graphics) {
        PCoordinates start = textArea.getCoordinates(getStartIndex());
        PCoordinates end = textArea.getCoordinates(getEndIndex());
        paint(graphics, start, end);
    }
    
    /**
     * Override this method to specify how the highlight may be painted.  The painting
     * must occur within the FontMetrics of any given character.
     */
    public abstract void paint(Graphics2D graphics, PCoordinates start, PCoordinates end);
}
