package e.ptextarea;

import java.util.*;

/**
 * Styles the text of emails.
 * 
 * @author Phil Norman
 */
public class PEmailTextStyler extends PAbstractTextStyler {
    private static final String QUOTING_CHARACTERS = ">";
    private static final PStyle[] QUOTE_STYLES = new PStyle[] { PStyle.COMMENT };
    
    public PEmailTextStyler(PTextArea textArea) {
        super(textArea);
        textArea.setTextStyler(this);
    }
    
    private int getLeadingQuoteCharacterCount(String line) {
        // FIXME: it's probably a mistake to count "                       > 4" as being quoted. if the first character on a line isn't a quote character, the line isn't quoted. this was more obvious when "|" was considered a quote character because Ruby source often contains lines like "  |line|" which we'd mistakenly color.
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
        // FIXME: this doesn't work for Outlook-style "-----Original Message-----"-quoted mail.
        // FIXME: the attempt to give the "On ...:" line the same quote level as the corresponding quoted text doesn't work well with mail sent by Mail.app because it tends to leave an empty line between the "On ... :" line and the first quoted line.
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
        int quoteLevel = getQuoteLevel(lineIndex, line);
        if (quoteLevel > 0) {
            // FIXME: this didn't work as intended because of an off-by-one error, but it doesn't matter while we only have one QUOTE_STYLE anyway.
            style = QUOTE_STYLES[quoteLevel % QUOTE_STYLES.length];
        }
        result.add(new PTextSegment(textArea, lineStartOffset, lineStartOffset + line.length(), style));
        return result;
    }
    
    public String[] getKeywords() {
        return new String[0];
    }
}
