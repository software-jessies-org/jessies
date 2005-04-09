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
    
    /**
     * Optionally handles the given mouse click event.  This is called when a single click occurs on
     * the text component.  If the styler handles the event, it should consume it.
     */
    public void mouseClicked(MouseEvent event, int clickLocation) {
        CharSequence hyperlinkText = getHyperlinkCharSequenceAt(clickLocation);
        if (hyperlinkText != null) {
            hyperlinkClicked(hyperlinkText);
            event.consume();
        }
    }
    
    public void hyperlinkClicked(CharSequence hyperlinkText) {
        System.out.println("Hyperlink clicked: " + hyperlinkText);
    }
    
    /**
     * Optionally returns a special mouse cursor to use when over the given location.  A null
     * return means that the default cursor should be used.
     */
    public Cursor getCursorForPosition(int textLocation) {
        if (getHyperlinkCharSequenceAt(textLocation) == null) {
            return null;
        } else {
            return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        }
    }
    
    private CharSequence getHyperlinkCharSequenceAt(int textLocation) {
        int lineIndex = textArea.getLineOfOffset(textLocation);
        int lineStart = textArea.getLineStartOffset(lineIndex);
        int lineEnd = textArea.getLineEndOffset(lineIndex);
        CharSequence line = textArea.getPTextBuffer().subSequence(lineStart, lineEnd);
        Matcher matcher = highlightPattern.matcher(line);
        int offsetInLine = textLocation - lineStart;
        while (matcher.find()) {
            if (offsetInLine >= matcher.start() && offsetInLine < matcher.end()) {
                return line.subSequence(matcher.start(), matcher.end());
            } else if (offsetInLine < matcher.start()) {
                return null;  // Gone past - definitely no more work to do.
            }
        }
        return null;
    }
}
