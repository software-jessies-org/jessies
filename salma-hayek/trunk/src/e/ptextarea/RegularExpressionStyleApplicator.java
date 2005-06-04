package e.ptextarea;

import java.util.*;
import java.util.regex.*;

/**
 * Styles any chunk of text matching a regular expression. 
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
    
    public List<PTextSegment> applyStylingTo(PTextSegment segment) {
        ArrayList<PTextSegment> result = new ArrayList<PTextSegment>();
        Matcher matcher = pattern.matcher(segment.getCharSequence());
        int normalStart = 0;
        int offset = segment.getOffset();
        while (matcher.find()) {
            if (isGoodMatch(matcher)) {
                if (matcher.start() > normalStart) {
                    result.add((PTextSegment) segment.subSegment(normalStart, matcher.start()));
                }
                result.add(new PTextSegment(textArea, offset + matcher.start(), offset + matcher.end(), style));
                normalStart = matcher.end();
            }
        }
        if (segment.getText().length() > normalStart) {
            result.add((PTextSegment) segment.subSegment(normalStart));
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
    public boolean isGoodMatch(Matcher matcher) {
        return true;
    }
}
