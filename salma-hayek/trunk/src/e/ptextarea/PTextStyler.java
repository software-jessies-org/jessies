package e.ptextarea;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * A PTextStyler is a thing which knows how to apply styles to lines of text.  This is used for
 * syntax highlighting.
 * 
 * @author Phil Norman
 */
public interface PTextStyler {
    /**
     * Returns a series of segments of text describing how to render each part of the
     * specified logical line.
     */
    public List<PTextSegment> getTextSegments(int lineIndex);
    
    /**
     * Optionally handles the given mouse click event.  This is called when a single click occurs on
     * the text component.  If the styler handles the event, it should consume it.
     */
    public void mouseClicked(MouseEvent event, int offset);
    
    /**
     * Returns the mouse cursor to use when over the given location, or null
     * to use the default cursor.
     */
    public Cursor getCursorForLocation(Point point);
    
    /**
     * Returns the tool-tip to use when over the given location, or null for
     * no tool-tip.
     */
    public String getToolTipForLocation(Point point);
    
    /**
     * Adds this language's keywords to the given collection. This lets
     * something like a spelling checker automatically share the knowledge of
     * the keywords.
     */
    public void addKeywordsTo(Collection<String> collection);
}
