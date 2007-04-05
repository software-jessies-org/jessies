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
        PAnchorSet anchorSet = textArea.getTextBuffer().getAnchorSet();
        anchorSet.add(start);
        anchorSet.add(end);
    }
    
    public int getStartIndex() {
        return start.getIndex();
    }
    
    public int getEndIndex() {
        return end.getIndex();
    }
    
    /**
     * Detaches the anchors marking the bounds of this highlight from the PTextArea.
     * This *must* be called in order that the highlight is properly cleared up.
     */
    public void detachAnchors() {
        PAnchorSet anchorSet = textArea.getTextBuffer().getAnchorSet();
        anchorSet.remove(start);
        anchorSet.remove(end);
    }
    
    public void paint(Graphics2D g) {
        PCoordinates start = textArea.getCoordinates(getStartIndex());
        PCoordinates end = textArea.getCoordinates(getEndIndex());
        
        // Work out which lines the highlight actually needs to bother to paint.
        // If you select a million-line document, for example, the selection highlight should only paint the visible lines.
        final int lineHeight = textArea.getLineHeight();
        Rectangle bounds = g.getClipBounds();
        Insets insets = textArea.getInsets();
        final int minVisibleLine = (bounds.y - insets.top) / lineHeight;
        final int maxVisibleLine = (bounds.y - insets.top + bounds.height) / lineHeight;
        final int firstLineIndex = Math.max(minVisibleLine, start.getLineIndex());
        final int lastLineIndex = Math.min(maxVisibleLine, end.getLineIndex());
        
        paintHighlight(g, start, end, insets, lineHeight, firstLineIndex, lastLineIndex);
    }
    
    /**
     * Override this method to specify how the highlight may be painted.
     * The painting must occur within the FontMetrics of any given character.
     */
    protected abstract void paintHighlight(Graphics2D g, PCoordinates start, PCoordinates end, Insets insets, int lineHeight, int firstLineIndex, int lastLineIndex);
    
    /**
     * Override this at the lowest level possible in order to provide the name of the
     * object generating this type of highlight.
     */
    public abstract String getHighlighterName();
    
    public String toString() {
        return "PHighlight[start=" + getStartIndex() + ",end=" + getEndIndex() + "]";
    }

    public class HighlightAnchor extends PAnchor {
        public HighlightAnchor(int index) {
            super(index);
        }
        
        @Override
        public void anchorDestroyed() {
            textArea.removeHighlight(PHighlight.this);
        }
        
        @Override
        public String toString() {
            return "HighlightAnchor[" + getIndex() + ", " + PHighlight.this + "]";
        }
    }
}
