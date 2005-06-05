package e.ptextarea;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
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
    
    public List<PTextSegment> getTextSegments(int lineIndex) {
        ArrayList<PTextSegment> result = new ArrayList<PTextSegment>();
        CharSequence line = textArea.getLineContents(lineIndex);
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
        return result;
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
}
