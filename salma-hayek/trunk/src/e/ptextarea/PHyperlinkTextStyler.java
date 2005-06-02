package e.ptextarea;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.*;

/**
 * Styles capturing group 1 from the given regular expression as a link, in
 * the traditional web browser way of using blue text and an underline.
 */
public abstract class PHyperlinkTextStyler extends PAbstractTextStyler {
    private Pattern highlightPattern;
    
    public PHyperlinkTextStyler(PTextArea textArea, String highlightPattern) {
        super(textArea);
        // FIXME: can we check that there is a capturing group 1?
        this.highlightPattern = Pattern.compile(highlightPattern);
    }
    
    public PTextSegment[] getTextSegments(int lineIndex) {
        ArrayList<PTextSegment> result = new ArrayList<PTextSegment>();
        String line = textArea.getLineContents(lineIndex).toString();
        int lineStart = textArea.getLineStartOffset(lineIndex);
        Matcher matcher = highlightPattern.matcher(line);
        int lastStart = 0;
        while (matcher.find() && isAcceptableMatch(line, matcher)) {
            result.add(new PTextSegment(textArea, lineStart + lastStart, lineStart + matcher.start(1), PStyle.NORMAL));
            PTextSegment linkSegment = new PUnderlinedTextSegment(textArea, lineStart + matcher.start(1), lineStart + matcher.end(1), PStyle.HYPERLINK);
            linkSegment.setToolTip(makeToolTip(matcher));
            result.add(linkSegment);
            lastStart = matcher.end(1);
        }
        if (lastStart < line.length()) {
            result.add(new PTextSegment(textArea, lineStart + lastStart, lineStart + line.length(), PStyle.NORMAL));
        }
        return result.toArray(new PTextSegment[result.size()]);
    }
    
    /**
     * Optionally handles the given mouse click event.  This is called when a single click occurs on
     * the text component.  If the styler handles the event, it should consume it.
     */
    public void mouseClicked(MouseEvent event, int offset) {
        Iterator<PLineSegment> it = textArea.getLogicalSegmentIterator(offset);
        if (it.hasNext()) {
            PLineSegment segment = it.next();
            if (segment.getStyle() == PStyle.HYPERLINK) {
                CharSequence chars = segment.getCharSequence();
                Matcher matcher = highlightPattern.matcher(chars);
                matcher.matches(); // FIXME: what if this returns false?
                hyperlinkClicked(chars, matcher);
                event.consume();
            }
        }
    }
    
    /**
     * Override this to implement whatever behavior you want for a clicked-on
     * link.
     */
    public abstract void hyperlinkClicked(CharSequence hyperlinkText, Matcher matcher);
    
    /**
     * Override this to perform any extra processing that can't be done by a
     * regular expression.
     */
    public abstract boolean isAcceptableMatch(CharSequence line, Matcher matcher);
    
    /**
     * Override this to return a custom tool-tip for your link.
     */
    public String makeToolTip(Matcher matcher) {
       return null;
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
}
