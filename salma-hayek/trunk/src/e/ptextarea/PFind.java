package e.ptextarea;

import java.awt.*;

public class PFind {
    /**
     * Used to mark the matches in the text as if they'd been gone over with a highlighter pen. We use
     * full yellow with half-alpha so you can see the selection through, as a dirty smudge, just like a real
     * highlighter pen might do.
     */
    public static class MatchHighlight extends PColoredHighlight {
        private static final Color MATCH_COLOR = new Color(255, 255, 0, 128);
        
        public MatchHighlight(PTextArea textArea, int startIndex, int endIndex) {
            super(textArea, startIndex, endIndex, MATCH_COLOR);
        }
    }
    
    public static class MatchHighlightMatcher implements PHighlightMatcher {
        private boolean forwards;
        private int selectionStart;
        private int selectionEnd; 
        
        public MatchHighlightMatcher(boolean forwards, PTextArea textArea) {
            this.forwards = forwards;
            this.selectionStart = textArea.getSelectionStart();
            this.selectionEnd = textArea.getSelectionEnd();
        }
        
        public boolean matches(PHighlight highlight) {
            if (highlight instanceof MatchHighlight == false) {
                return false;
            }
            final int minOffset = Math.min(highlight.getStartIndex(), highlight.getEndIndex());
            final int maxOffset = Math.max(highlight.getStartIndex(), highlight.getEndIndex());
            if (forwards) {
                return minOffset > selectionEnd;
            } else {
                return maxOffset < selectionStart;
            }
        }
    }
}
