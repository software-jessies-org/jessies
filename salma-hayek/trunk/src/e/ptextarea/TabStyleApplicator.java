package e.ptextarea;

import java.util.*;
import java.util.regex.*;

/**
 * Recognizes runs of ASCII HT characters.
 */
public class TabStyleApplicator extends RegularExpressionStyleApplicator {
    public TabStyleApplicator(PTextArea textArea) {
        super(textArea, "(\t+)", PStyle.NORMAL);
    }
    
    @Override
    protected PLineSegment makeNewSegment(PTextArea textArea, Matcher matcher, int start, int end, PStyle style) {
        return new PTabSegment(textArea, start, end);
    }
}
