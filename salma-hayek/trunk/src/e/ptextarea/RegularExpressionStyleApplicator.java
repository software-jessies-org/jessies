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
    private static final EnumSet<PStyle> SOURCE_STYLES = EnumSet.of(PStyle.NORMAL);
    private PTextArea textArea;
    private Pattern pattern;
    private PStyle style;
    
    public RegularExpressionStyleApplicator(PTextArea textArea, String regularExpression, PStyle style) {
        this.textArea = textArea;
        this.pattern = Pattern.compile(regularExpression);
        this.style = style;
    }
    
    public List<PTextSegment> applyStylingTo(String line, PTextSegment segment) {
        ArrayList<PTextSegment> result = new ArrayList<PTextSegment>();
        Matcher matcher = pattern.matcher(segment.getCharSequence());
        int normalStart = 0;
        int offset = segment.getOffset();
        while (matcher.find()) {
            if (isAcceptableMatch(line, matcher)) {
                final int matchStart = matcher.start(1);
                final int matchEnd = matcher.end(1);
                if (matchStart > normalStart) {
                    result.add((PTextSegment) segment.subSegment(normalStart, matchStart));
                }
                result.add(makeNewSegment(textArea, matcher, offset + matchStart, offset + matchEnd, style));
                normalStart = matchEnd;
            }
        }
        if (segment.getModelTextLength() > normalStart) {
            result.add((PTextSegment) segment.subSegment(normalStart));
        }
        return result;
    }
    
    protected PTextSegment makeNewSegment(PTextArea textArea, Matcher matcher, int start, int end, PStyle style) {
        // FIXME: underlining should be a PStyle attribute.
        if (style == PStyle.HYPERLINK) {
            PTextSegment result = new PUnderlinedTextSegment(textArea, start, end, style);
            configureSegment(result, matcher);
            return result;
        } else {
            return new PTextSegment(textArea, start, end, style);
        }
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
    
    public EnumSet<PStyle> getSourceStyles() {
        return SOURCE_STYLES;
    }
    
    /**
     * Override this to configure each matching text segment.
     */
    protected void configureSegment(PTextSegment segment, Matcher matcher) {
    }
}
