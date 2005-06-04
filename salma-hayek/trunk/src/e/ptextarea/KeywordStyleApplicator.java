package e.ptextarea;

import java.util.*;
import java.util.regex.*;

/**
 * Recognizes keywords within NORMAL text segments and styles them KEYWORD.
 */
public class KeywordStyleApplicator implements StyleApplicator {
    private PTextArea textArea;
    private HashSet<String> keywords;
    private static final Pattern KEYWORD_PATTERN = Pattern.compile("\\b\\w+\\b");
    
    public KeywordStyleApplicator(PTextArea textArea, HashSet<String> keywords) {
        this.textArea = textArea;
        this.keywords = keywords;
    }
    
    public List<PTextSegment> applyStylingTo(PTextSegment segment) {
        ArrayList<PTextSegment> result = new ArrayList<PTextSegment>();
        String text = segment.getText();
        Matcher matcher = KEYWORD_PATTERN.matcher(text);
        int normalStart = 0;
        int offset = segment.getOffset();
        while (matcher.find()) {
            String keyword = matcher.group();
            if (keywords.contains(keyword)) {
                if (matcher.start() > normalStart) {
                    result.add((PTextSegment) segment.subSegment(normalStart, matcher.start()));
                }
                result.add(new PTextSegment(textArea, offset + matcher.start(), offset + matcher.end(), PStyle.KEYWORD));
                normalStart = matcher.end();
            }
        }
        if (segment.getText().length() > normalStart) {
            result.add((PTextSegment) segment.subSegment(normalStart));
        }
        return result;
    }
}
