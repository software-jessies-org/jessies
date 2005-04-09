package e.ptextarea;


import java.awt.*;
import java.awt.event.*;
import java.util.*;

import java.util.List;

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
    
    /**
     * Returns a series of segments of text describing how to render each part of the
     * specified line.
     */
    public PTextSegment[] getLineSegments(PTextArea.SplitLine splitLine) {
        int lineIndex = splitLine.getLineIndex();
        String fullLine = textArea.getLineList().getLine(lineIndex).getContents().toString();
        List segments = getLineSegments(lineIndex, fullLine);
        int index = 0;
        ArrayList result = new ArrayList();
        int start = splitLine.getOffset();
        int end = start + splitLine.getLength();
        
        for (int i = 0; index < end && i < segments.size(); ++i) {
            PTextSegment segment = (PTextSegment) segments.get(i);
            if (start >= index + segment.getLength()) {
                index += segment.getLength();
                continue;
            }
            if (start > index) {
                int skip = start - index;
                segment = segment.subSegment(skip);
                index += skip;
            }
            if (end < index + segment.getLength()) {
                segment = segment.subSegment(0, end - index);
            }
            result.add(segment);
            index += segment.getLength();
        }
        return (PTextSegment[]) result.toArray(new PTextSegment[result.size()]);
    }
    
    public abstract List getLineSegments(int lineIndex, String line);
    
    /** Returns the color associated with an indexed style. */
    public abstract Color getColorForStyle(int style);
    
    /**
     * Optionally handles the given mouse click event.  This is called when a single click occurs on
     * the text component.  If the styler handles the event, it should consume it.
     */
    public void mouseClicked(MouseEvent event, int clickLocation) { }
    
    /**
     * Optionally returns a special mouse cursor to use when over the given location.  A null
     * return means that the default cursor should be used.
     */
    public Cursor getCursorForPosition(int textLocation) {
        return null;
    }
}
