package e.ptextarea;

import e.gui.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.*;

/**
 * Styles any chunk of text matching a regular expression. Capturing group 1 is
 * used as the range to be styled.
 */
public class RegularExpressionStyleApplicator implements StyleApplicator {
    private PTextArea textArea;
    private Pattern pattern;
    private PStyle style;
    
    public RegularExpressionStyleApplicator(PTextArea textArea, String regularExpression, PStyle style) {
        this.textArea = textArea;
        this.pattern = Pattern.compile(regularExpression);
        this.style = style;
    }
    
    public List<PLineSegment> applyStylingTo(String line, PLineSegment segment) {
        ArrayList<PLineSegment> result = new ArrayList<PLineSegment>();
        Matcher matcher = pattern.matcher(segment.getCharSequence());
        int normalStart = 0;
        int offset = segment.getOffset();
        while (matcher.find()) {
            if (isAcceptableMatch(line, matcher)) {
                final int matchStart = matcher.start(1);
                final int matchEnd = matcher.end(1);
                if (matchStart > normalStart) {
                    result.add(segment.subSegment(normalStart, matchStart));
                }
                result.add(makeNewSegment(textArea, matcher, offset + matchStart, offset + matchEnd, style));
                normalStart = matchEnd;
            }
        }
        if (segment.getModelTextLength() > normalStart) {
            result.add(segment.subSegment(normalStart));
        }
        return result;
    }
    
    protected PLineSegment makeNewSegment(PTextArea textArea, Matcher matcher, int start, int end, PStyle style) {
        PTextSegment result = new PTextSegment(textArea, start, end, style);
        if (style == PStyle.HYPERLINK) {
            configureSegment(result, matcher);
        }
        return result;
    }
    
    /**
     * Override this if you need to make a test that you can't express in the
     * regular expression. The keyword styler, for example, uses a regular
     * expression that matches any word and then checks whether the word is
     * a keyword here. That works around Java 1.5 performance problems with
     * regular expressions that are long sequences like "a|b|...|y|z".
     */
    public boolean isAcceptableMatch(CharSequence line, Matcher matcher) {
        return true;
    }
    
    public boolean canApplyStylingTo(PStyle style) {
        return (style == PStyle.NORMAL);
    }
    
    /**
     * Override this to configure each matching text segment.
     */
    protected void configureSegment(PTextSegment segment, Matcher matcher) {
    }
}
