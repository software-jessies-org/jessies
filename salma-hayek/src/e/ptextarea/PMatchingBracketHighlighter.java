package e.ptextarea;

import java.awt.*;
import java.util.*;

/**
 * Highlights matching pairs of bracket characters in a text area.
 * Will also highlight a mis-matched single bracket, in a different color.
 */
public class PMatchingBracketHighlighter implements PCaretListener {
    private static final Color MATCH_COLOR = new Color(0.10f, 0.78f, 0.10f, 0.5f);
    private static final Color FAILED_MATCH_COLOR = new Color(0.78f, 0.10f, 0.10f, 0.5f);
    private static final String HIGHLIGHTER_NAME = "PMatchingBracketHighlighter";

    private final ArrayList<PColoredHighlight> highlights;
    
    public PMatchingBracketHighlighter() {
        this.highlights = new ArrayList<PColoredHighlight>();
    }
    
    public void caretMoved(PTextArea textArea, int selectionStart, int selectionEnd) {
        recalculateHighlights(textArea, selectionStart, selectionEnd);
        for (PHighlight highlight : highlights) {
            textArea.addHighlight(highlight);
        }
    }
    
    private void recalculateHighlights(PTextArea textArea, int selectionStart, int selectionEnd) {
        removeHighlights(textArea);
        if (selectionStart != selectionEnd) {
            return;
        }
        
        final int offset = selectionStart;
        
        // Look for a bracket to match with.
        if (PBracketUtilities.beforeCloseBracket(textArea.getTextBuffer(), offset)) {
            highlights.add(new MatchingBracketHighlight(textArea, offset, offset + 1));
        }
        if (PBracketUtilities.afterOpenBracket(textArea.getTextBuffer(), offset)) {
            highlights.add(new MatchingBracketHighlight(textArea, offset - 1, offset));
        }
        
        if (highlights.isEmpty() || highlights.size() == 2) {
            // We're not next to a bracket, or we're between two matching brackets, so we've nothing to match.
            return;
        }
        
        // Try to find a match.
        int matchingBracketOffset = PBracketUtilities.findMatchingBracketInSameStyle(textArea, offset);
        if (matchingBracketOffset != -1) {
            int start = matchingBracketOffset;
            int end = start + 1;
            highlights.add(new MatchingBracketHighlight(textArea, start, end));
        } else {
            highlights.get(0).setColor(FAILED_MATCH_COLOR);
        }
    }
    
    private void removeHighlights(PTextArea textArea) {
        for (PHighlight highlight : highlights) {
            textArea.removeHighlight(highlight);
        }
        highlights.clear();
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
