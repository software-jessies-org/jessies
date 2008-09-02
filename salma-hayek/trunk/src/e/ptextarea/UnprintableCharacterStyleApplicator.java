package e.ptextarea;

import java.util.regex.*;

/**
 * Recognizes unprintable characters.
 */
public class UnprintableCharacterStyleApplicator extends RegularExpressionStyleApplicator {
    private static final Pattern UNPRINTABLE_CHARACTER_PATTERN = Pattern.compile("([\\u0000-\\u0008\\u000a-\\u001f\\u007f]+)");
    
    public UnprintableCharacterStyleApplicator(PTextArea textArea) {
        super(textArea, UNPRINTABLE_CHARACTER_PATTERN, PStyle.UNPRINTABLE);
    }
    
    @Override
    protected PTextSegment makeNewSegment(PTextArea textArea, Matcher matcher, int start, int end, PStyle style) {
        return new UnprintableCharacterTextSegment(textArea, start, end, style);
    }
}
