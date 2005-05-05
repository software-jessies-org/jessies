package e.ptextarea;


import java.awt.*;

/**
 * A PHighlight is an anchored region of text which is specially painted by PTextArea.
 * The PHighlight is automatically destroyed when either of its start and end points is
 * destroyed.  Painting is performed by implementing the abstract paint method.
 *
 * @author Phil Norman
 */

public abstract class PHighlight {
    protected PTextArea textArea;
    private PAnchor start;
    private PAnchor end;
   
    public PHighlight(PTextArea textArea, int startIndex, int endIndex) {
        this.textArea = textArea;
        if (endIndex < startIndex) {
            throw new IndexOutOfBoundsException("The end index (" + endIndex + ") may not be less than the start index (" + startIndex + ")");
        }
        start = new HighlightAnchor(startIndex);
        end = new HighlightAnchor(endIndex);
        textArea.getAnchorSet().add(start);
        textArea.getAnchorSet().add(end);
    }
    
    public int getStartIndex() {
        return start.getIndex();
    }
    
    public int getEndIndex() {
        return end.getIndex();
    }
    
    /**
     * Removes this highlight from the PTextArea.  This method is called when one or other
     * of the end points of the highlight is destroyed.
     */
    public void delete() {
        textArea.removeHighlight(this);
    }
    
    /**
     * Detaches the anchors marking the bounds of this highlight from the PTextArea.
     * This *must* be called in order that the highlight is properly cleared up.
     */
    public void detachAnchors() {
        textArea.getAnchorSet().remove(start);
        textArea.getAnchorSet().remove(end);
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
    
    public String toString() {
        return "PHighlight[start=" + getStartIndex() + ",end=" + getEndIndex() + "]";
    }

    private class HighlightAnchor extends PAnchor {
        public HighlightAnchor(int index) {
            super(index);
        }
        
        public void delete() {
            PHighlight.this.delete();
        }
        
        public String toString() {
            return "HighlightAnchor[" + getIndex() + ", " + PHighlight.this + "]";
        }
    }
}
