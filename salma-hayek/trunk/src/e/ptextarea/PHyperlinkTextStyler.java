package e.ptextarea;


import java.awt.*;
import java.util.*;
import java.util.regex.*;

import java.util.List;

/**
 * A PHyperlinkTextStyler is a thing which knows how to apply hyperlinks to text.
 * 
 * @author Phil Norman
 */

public class PHyperlinkTextStyler extends PAbstractTextStyler {
    private static final int NORMAL_STYLE = 0;
    private static final int HYPERLINK_STYLE = 1;
    
    private Pattern highlightPattern;
    
    public PHyperlinkTextStyler(PTextArea textArea, String highlightPattern) {
        super(textArea);
        this.highlightPattern = Pattern.compile(highlightPattern);
    }
    
    public List getLineSegments(int lineIndex, String line) {
        ArrayList result = new ArrayList();
        Matcher matcher = highlightPattern.matcher(line);
        int lastStart = 0;
        while (matcher.find()) {
            result.add(new PTextSegment(NORMAL_STYLE, line.substring(lastStart, matcher.start())));
            result.add(new PUnderlinedTextSegment(HYPERLINK_STYLE, line.substring(matcher.start(), matcher.end())));
            lastStart = matcher.end();
        }
        if (lastStart < line.length()) {
            result.add(new PTextSegment(NORMAL_STYLE, line.substring(lastStart)));
        }
        return result;
    }
    
    /** Returns the color associated with an indexed style. */
    public Color getColorForStyle(int style) {
        return (style == 0) ? Color.BLACK : Color.BLUE;
    }
}
