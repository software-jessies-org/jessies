package e.ptextarea;

import java.awt.*;
import java.awt.event.*;

/**
 * A trivial styler for plain text, which colors all text black.
 */
public class PPlainTextStyler implements PTextStyler {
    private static final PTextSegment[] EMPTY_LINE_SEGMENTS = new PTextSegment[0];

    public static final PPlainTextStyler INSTANCE = new PPlainTextStyler();

    private PPlainTextStyler() {
        // Only one instance.
    }

    public PTextSegment[] getLineSegments(PTextArea.SplitLine splitLine) {
        if (splitLine.getLength() == 0) {
            return EMPTY_LINE_SEGMENTS;
        }
        return new PTextSegment[] {
            new PTextSegment(0, splitLine.getContents().toString())
        };
    }

    public Color getColorForStyle(int style) {
        return Color.BLACK;
    }
    
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
