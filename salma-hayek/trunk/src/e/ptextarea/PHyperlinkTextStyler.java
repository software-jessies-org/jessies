package e.ptextarea;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.*;

import java.util.List;

/**
 * A PHyperlinkTextStyler is a thing which knows how to apply hyperlinks to text.
 * 
 * @author Phil Norman
 */

public abstract class PHyperlinkTextStyler extends PAbstractTextStyler {
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
        while (matcher.find() && isAcceptableMatch(line, matcher)) {
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
    
    /**
     * Optionally handles the given mouse click event.  This is called when a single click occurs on
     * the text component.  If the styler handles the event, it should consume it.
     */
    public void mouseClicked(MouseEvent event, int offset) {
        PLineSegment segment = textArea.getLineSegmentAtLocation(event.getPoint());
        if (segment instanceof PTextSegment && segment.getStyleIndex() == HYPERLINK_STYLE) {
            hyperlinkClicked(((PTextSegment) segment).getSuperSegment().getText());
            event.consume();
        }
    }
    
    /**
     * Override this to implement whatever behavior you want for a clicked-on
     * link.
     */
    public abstract void hyperlinkClicked(CharSequence hyperlinkText);
    
    /**
     * Override this to perform any extra processing that can't be done by a
     * regular expression.
     */
    public abstract boolean isAcceptableMatch(CharSequence line, Matcher matcher);
    
    /**
     * Optionally returns a special mouse cursor to use when over the given location.  A null
     * return means that the default cursor should be used.
     */
    public Cursor getCursorForLocation(Point point) {
        PLineSegment segment = textArea.getLineSegmentAtLocation(point);
        if (segment != null && segment.getStyleIndex() == HYPERLINK_STYLE) {
            return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        } else {
            return null;
        }
    }
}
