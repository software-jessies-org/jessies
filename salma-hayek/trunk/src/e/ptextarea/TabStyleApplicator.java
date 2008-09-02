package e.ptextarea;

import java.util.regex.*;

/**
 * Recognizes runs of ASCII HT characters.
 */
public class TabStyleApplicator extends RegularExpressionStyleApplicator {
    private static final Pattern TAB_PATTERN = Pattern.compile("(\t+)");
    
    public TabStyleApplicator(PTextArea textArea) {
        super(textArea, TAB_PATTERN, PStyle.NORMAL);
    }
    
    @Override
    protected PLineSegment makeNewSegment(PTextArea textArea, Matcher matcher, int start, int end, PStyle style) {
        return new PTabSegment(textArea, start, end);
    }
    
    @Override
    public boolean canApplyStylingTo(PStyle style) {
        return true;
    }
}
