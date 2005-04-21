package e.ptextarea;


import java.awt.*;
import java.awt.event.*;

/**
 * A PAbstractTextStyler is a thing.
 * 
 * @author Phil Norman
 */

public abstract class PAbstractTextStyler implements PTextStyler {
    protected PTextArea textArea;
    
    public PAbstractTextStyler(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    public abstract PTextSegment[] getLineSegments(int line);
    
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
        return null;
    }
    
    /**
     * Optionally returns a special double-click handler for use when a double-click occurs at
     * the given position.  A null return value means the default handling should be performed.
     */
    public PDragHandler getDoubleClickDragHandler(int clickOffset) {
        return null;
    }
}
