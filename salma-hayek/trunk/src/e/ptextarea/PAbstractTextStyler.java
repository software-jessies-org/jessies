package e.ptextarea;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

public abstract class PAbstractTextStyler implements PTextStyler {
    protected PTextArea textArea;
    
    public PAbstractTextStyler(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    public abstract List<PTextSegment> getTextSegments(int line);
    
    /**
     * Optionally handles the given mouse click event.  This is called when a single click occurs on
     * the text component.  If the styler handles the event, it should consume it.
     */
    public void mouseClicked(MouseEvent event, int offset) {
        // Do nothing.
    }
    
    /**
     * Optionally returns a special mouse cursor to use when over the given location.  A null
     * return means that the default cursor should be used.
     */
    public Cursor getCursorForLocation(Point point) {
        PLineSegment segment = textArea.getLineSegmentAtLocation(point);
        if (segment != null && segment.getStyle() == PStyle.HYPERLINK) {
            return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        } else {
            return null;
        }
    }
    
    public String getToolTipForLocation(Point point) {
        PLineSegment segment = textArea.getLineSegmentAtLocation(point);
        if (segment != null && segment instanceof PTextSegment) {
            return ((PTextSegment) segment).getToolTip();
        } else {
            return null;
        }
    }
}
