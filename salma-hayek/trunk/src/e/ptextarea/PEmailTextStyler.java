package e.ptextarea;

import java.util.*;
import java.util.regex.*;
import e.util.*;

/**
 * Styles the text of emails.
 * 
 * @author Phil Norman
 */
public class PEmailTextStyler extends PAbstractTextStyler {
    private static final String QUOTING_CHARACTERS = ">";
    private static final PStyle[] QUOTE_STYLES = new PStyle[] { PStyle.COMMENT };
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("\n-- \n");
    
    private int signatureLineIndex = Integer.MAX_VALUE;
    
    public PEmailTextStyler(PTextArea textArea) {
        super(textArea);
        textArea.setTextStyler(this);
        initTextListener();
        findSignatureLine();
    }
    
    private void findSignatureLine() {
        // FIXME: few people use "-- " to introduce signatures any more.
        // FIXME: some of those that do try actually use "--" instead.
        // FIXME: this doesn't cope with mailing list digests, because it doesn't realize that a signature isn't necessarily the last thing in a mail.
        // FIXME: see also the problem described in getTextSegments.
        /*
        Matcher matcher = SIGNATURE_PATTERN.matcher(textArea.getTextBuffer());
        int newIndex = matcher.find() ? textArea.getLineOfOffset(matcher.start()) : Integer.MAX_VALUE;
        if (signatureLineIndex != newIndex) {
            signatureLineIndex = newIndex;
            textArea.repaint();
        }
        */
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
        if (lineIndex > signatureLineIndex) {
            // FIXME: this is a bad idea because PREPROCESSOR is meaningful to PTextArea and in particular means that the URLs and email addresses people put in their signatures become unclickable.
            // style = PStyle.PREPROCESSOR;
        } else {
            int quoteLevel = getQuoteLevel(lineIndex, line);
            if (quoteLevel > 0) {
                // FIXME: this didn't work as intended because of an off-by-one error, but it doesn't matter while we only have one QUOTE_STYLE anyway.
                style = QUOTE_STYLES[quoteLevel % QUOTE_STYLES.length];
            }
        }
        result.add(new PTextSegment(textArea, lineStartOffset, lineStartOffset + line.length(), style));
        return result;
    }
    
    public void addKeywordsTo(Collection<String> collection) { }
}
