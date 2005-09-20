package e.ptextarea;

import java.awt.*;

/**
 * Highlights matching pairs of bracket characters in a text area.
 * Will also highlight a mis-matched single bracket, in a different color.
 */
public class PMatchingBracketHighlighter implements PCaretListener {
    private static final Color MATCH_COLOR = new Color(0.10f, 0.78f, 0.10f, 0.5f);
    private static final Color FAILED_MATCH_COLOR = new Color(0.78f, 0.10f, 0.10f, 0.5f);
    private static final String HIGHLIGHTER_NAME = "PMatchingBracketHighlighter";

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
        
        // The same-style char sequence is expensive, but there's a cheap way
        // to exclude most cases where we don't need it:
        CharSequence chars = textArea.getTextBuffer();
        if (PBracketUtilities.isNextToBracket(chars, offset) == false) {
            return;
        }
        
        // Look for a bracket to match with.
        if (PBracketUtilities.beforeCloseBracket(chars, offset)) {
            highlights[0] = new MatchingBracketHighlight(textArea, offset, offset + 1);
        } else if (PBracketUtilities.afterOpenBracket(chars, offset)) {
            highlights[0] = new MatchingBracketHighlight(textArea, offset - 1, offset);
        }
        
        if (highlights[0] == null) {
            // We're not next to a bracket, so we've nothing to match.
            return;
        }
        
        // Try to find a match.
        int matchingBracketOffset = PBracketUtilities.findMatchingBracketInSameStyle(textArea, offset);
        if (matchingBracketOffset != -1) {
            int start = matchingBracketOffset;
            int end = start + 1;
            highlights[1] = new MatchingBracketHighlight(textArea, start, end);
        } else {
            highlights[0].setColor(FAILED_MATCH_COLOR);
        }
        
        // Add any highlights now. We may only have one, if we detected a
        // mis-match.
        for (PHighlight highlight : highlights) {
            if (highlight != null) {
                textArea.addHighlight(highlight);
            }
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
        public MatchingBracketHighlight(PTextArea textArea, int startIndex, int endIndex) {
            super(textArea, startIndex, endIndex, MATCH_COLOR);
        }
        
        public String getHighlighterName() {
            return HIGHLIGHTER_NAME;
        }
    }
}
