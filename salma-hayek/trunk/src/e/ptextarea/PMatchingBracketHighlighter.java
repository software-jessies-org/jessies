package e.ptextarea;

import java.awt.*;

/**
 * Highlights matching pairs of bracket characters in a text area.
 */
public class PMatchingBracketHighlighter implements PCaretListener {
    private static final Color FAILED_MATCH_COLOR = new Color(0.78f, 0.10f, 0.10f, 0.5f);
    
    private PTextArea textArea;
    private PColoredHighlight[] highlights = new PColoredHighlight[2];
    
    public PMatchingBracketHighlighter(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    public void caretMoved(PTextArea textArea, int selectionStart, int selectionEnd) {
        removeHighlights();
        if (selectionStart != selectionEnd) {
            return;
        }
        
        int offset = selectionStart;
        CharSequence chars = textArea.getTextBuffer();
        if (offset == chars.length()) {
            return;
        }
        
        char ch = chars.charAt(offset);
        if (PBracketUtilities.isCloseBracket(ch)) {
            highlights[0] = new MatchingBracketHighlight(textArea, offset, offset + 1);
            textArea.addHighlight(highlights[0]);
        } else if (offset > 0) {
            char previousChar = chars.charAt(offset - 1);
            if (PBracketUtilities.isOpenBracket(previousChar)) {
                highlights[0] = new MatchingBracketHighlight(textArea, offset - 1, offset);
                textArea.addHighlight(highlights[0]);
            }
        }
        if (highlights[0] == null) {
            return;
        }
        
        int matchingBracketOffset = PBracketUtilities.findMatchingBracket(chars, offset);
        if (matchingBracketOffset != -1) {
            int start = matchingBracketOffset;
            int end = start + 1;
            highlights[1] = new MatchingBracketHighlight(textArea, start, end);
            textArea.addHighlight(highlights[1]);
        } else {
            highlights[0].setColor(FAILED_MATCH_COLOR);
        }
    }
    
    private void removeHighlights() {
        for (int i = 0; i < highlights.length; ++i) {
            PHighlight highlight = highlights[i];
            if (highlight != null) {
                textArea.removeHighlight(highlight);
            }
            highlights[i] = null;
        }
    }
    
    public static class MatchingBracketHighlight extends PColoredHighlight {
        private static final Color MATCH_COLOR = new Color(0.78f, 0.98f, 0.78f);
        
        public MatchingBracketHighlight(PTextArea textArea, int startIndex, int endIndex) {
            super(textArea, startIndex, endIndex, MATCH_COLOR);
        }
    }
}
