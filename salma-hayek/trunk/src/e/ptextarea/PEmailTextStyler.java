package e.ptextarea;

import java.util.*;
import java.util.regex.*;
import e.util.*;

/**
 * A PEmailTextStyler styles the text of emails.  It understands how to
 * recognise signatures (which are coloured grey), and deals with quoted text
 * (in 3 repeating colours).
 * 
 * The signature finding code is a bit basic
 * 
 * 
 * @author Phil Norman
 */
public class PEmailTextStyler extends PAbstractTextStyler {
    private int signatureLineIndex = Integer.MAX_VALUE;
    private static final String SIGNATURE_START = "-- ";
    private static final String QUOTING_CHARACTERS = ">:|";
    
    private static final PStyle[] QUOTE_STYLES = new PStyle[] {
        PStyle.COMMENT, PStyle.STRING, PStyle.KEYWORD,
    };
    
    private static final PStyle SIGNATURE_STYLE = PStyle.PREPROCESSOR;
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("\n-- \n");
    
    public PEmailTextStyler(PTextArea textArea) {
        super(textArea);
        initTextListener();
        textArea.setTextStyler(this);
        findSignatureLine();
    }
    
    private void findSignatureLine() {
        Matcher matcher = SIGNATURE_PATTERN.matcher(textArea.getTextBuffer());
        int newIndex = matcher.find() ? textArea.getLineOfOffset(matcher.start()) : Integer.MAX_VALUE;
        if (signatureLineIndex != newIndex) {
            signatureLineIndex = newIndex;
            textArea.repaint();
        }
    }
    
    private void initTextListener() {
        textArea.getTextBuffer().addTextListener(new PTextListener() {
            public void textCompletelyReplaced(PTextEvent event) {
                findSignatureLine();
            }
            
            public void textInserted(PTextEvent event) {
                findSignatureLine();
            }
            
            public void textRemoved(PTextEvent event) {
                findSignatureLine();
            }
        });
    }
    
    private int getLeadingQuoteCharacterCount(String line) {
        int result = 0;
        for (int i = 0; i < line.length(); i++) {
            if (QUOTING_CHARACTERS.indexOf(line.charAt(i)) != -1) {
                result++;
            } else if (line.charAt(i) != ' ') {
                break;
            }
        }
        return result;
    }
    
    private int getQuoteLevel(int lineIndex, String line) {
        int result = getLeadingQuoteCharacterCount(line);
        if (line.endsWith(":") && (lineIndex < textArea.getLineCount() - 1)) {
            String lineBelow = textArea.getLineContents(lineIndex + 1).toString();
            if (getLeadingQuoteCharacterCount(lineBelow) == result + 1) {
                result++;
            }
        }
        return result;
    }
    
    public List<PLineSegment> getTextSegments(int lineIndex) {
        String line = textArea.getLineContents(lineIndex).toString();
        int lineStartOffset = textArea.getLineStartOffset(lineIndex);
        List<PLineSegment> result = new ArrayList<PLineSegment>();
        PStyle style = PStyle.NORMAL;
        if (lineIndex > signatureLineIndex) {
            style = SIGNATURE_STYLE;
        } else {
            int quoteLevel = getQuoteLevel(lineIndex, line);
            if (quoteLevel > 0) {
                style = QUOTE_STYLES[quoteLevel % QUOTE_STYLES.length];
            }
        }
        result.add(new PTextSegment(textArea, lineStartOffset, lineStartOffset + line.length(), style));
        return result;
    }
    
    public void addKeywordsTo(Collection<String> collection) { }
}
