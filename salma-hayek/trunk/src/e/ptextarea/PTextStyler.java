package e.ptextarea;


import java.awt.*;
import java.awt.event.*;

/**
 * A PTextStyler is a thing which knows how to apply styles to lines of text.  This is used for
 * syntax highlighting.
 * 
 * @author Phil Norman
 */

public interface PTextStyler {
    /**
     * Returns a series of segments of text describing how to render each part of the
     * specified line.
     */
    public PTextSegment[] getLineSegments(PTextArea.SplitLine splitLine);
    
    /** Returns the color associated with an indexed style. */
    public Color getColorForStyle(int style);
    
    /**
     * Optionally handles the given mouse click event.  This is called when a single click occurs on
     * the text component.  If the styler handles the event, it should consume it.
     */
    public void mouseClicked(MouseEvent event, int offset);
    
    /**
     * Optionally returns a special mouse cursor to use when over the given location.  A null
     * return means that the default cursor should be used.
     */
    public Cursor getCursorForLocation(Point point);
    
    /**
     * Optionally returns a special double-click handler for use when a double-click occurs at
     * the given position.  A null return value means the default handling should be performed.
     */
    public PDragHandler getDoubleClickDragHandler(int clickOffset);
}
