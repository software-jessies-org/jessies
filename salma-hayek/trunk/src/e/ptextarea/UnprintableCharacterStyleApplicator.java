package e.ptextarea;

import java.util.regex.*;

/**
 * Recognizes unprintable characters.
 */
public class UnprintableCharacterStyleApplicator extends RegularExpressionStyleApplicator {
    public UnprintableCharacterStyleApplicator(PTextArea textArea) {
        super(textArea, "([\\u0000-\\u0008\\u000a-\\u001f\\u007f]+)", PStyle.UNPRINTABLE);
    }
    
    @Override
    protected PTextSegment makeNewSegment(PTextArea textArea, Matcher matcher, int start, int end, PStyle style) {
        return new UnprintableCharacterTextSegment(textArea, start, end, style);
    }
}
