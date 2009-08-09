package e.ptextarea;

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
    public List<PLineSegment> getTextSegments(int lineIndex);
    
    /**
     * Returns the language's keywords.
     * This lets something like a spelling checker automatically share the knowledge of the keywords.
     */
    public String[] getKeywords();
}
