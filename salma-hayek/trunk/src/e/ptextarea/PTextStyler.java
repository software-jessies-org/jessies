package e.ptextarea;


import java.awt.*;

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
    public Color getDefaultColor(int style);
}
