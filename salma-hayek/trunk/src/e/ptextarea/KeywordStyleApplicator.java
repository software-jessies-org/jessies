package e.ptextarea;

import java.util.*;
import java.util.regex.*;

/**
 * Recognizes keywords within NORMAL text segments and styles them KEYWORD.
 */
public class KeywordStyleApplicator extends RegularExpressionStyleApplicator {
    private HashSet<String> keywords;
    
    public KeywordStyleApplicator(PTextArea textArea, HashSet<String> keywords) {
        super(textArea, "\\b\\w+\\b", PStyle.KEYWORD);
        this.keywords = keywords;
    }
    
    @Override
    public boolean isGoodMatch(Matcher matcher) {
        return keywords.contains(matcher.group());
    }
}
