package e.edit;

import java.awt.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
 * Highlights matching pairs of brackets in a JTextComponent. Add one of these
 * as a ChangeListener to your JTextComponent's Caret.
 */
public class MatchingBracketHighlighter implements ChangeListener {
    private static final Highlighter.HighlightPainter MATCHING_BRACKET_PAINTER = new DefaultHighlighter.DefaultHighlightPainter(new Color(0.78f, 0.98f, 0.78f));
    
    /**
     * We keep two matching bracket highlights; one for the bracket before
     * or after the caret, the other for its partner. Either or both may be
     * null. We use an array rather than two fields because (a) we don't
     * care which is which and shouldn't mislead by pretending to distinguish
     * them and (b) an array lets us avoid duplication when removing the
     * highlights.
     */
    private Object[] matchingBracketHighlights = new Object[2];
    
    private JTextComponent textComponent;
    
    public MatchingBracketHighlighter(JTextComponent textComponent) {
        this.textComponent = textComponent;
    }
    
    /** Implements the ChangeListener interface so we can watch. */
    public void stateChanged(ChangeEvent e) {
        updateMatchingBracketHighlights();
    }
    
    private void removeMatchingBracketHighlights() {
        for (int i = 0; i < matchingBracketHighlights.length; ++i) {
            if (matchingBracketHighlights[i] != null) {
                textComponent.getHighlighter().removeHighlight(matchingBracketHighlights[i]);
                matchingBracketHighlights[i] = null;
            }
        }
    }
    
    private void updateMatchingBracketHighlights() {
        removeMatchingBracketHighlights();
        
        try {
            int offset = textComponent.getCaretPosition();
            DocumentCharSequence chars = new DocumentCharSequence(textComponent.getDocument());
            Highlighter highlighter = textComponent.getHighlighter();
            char ch = chars.charAt(offset);
            if (Brackets.isCloseBracket(ch)) {
                matchingBracketHighlights[0] = highlighter.addHighlight(offset, offset + 1, MATCHING_BRACKET_PAINTER);
            } else if (offset > 0) {
                char previousChar = chars.charAt(offset - 1);
                if (Brackets.isOpenBracket(previousChar)) {
                    matchingBracketHighlights[0] = highlighter.addHighlight(offset - 1, offset, MATCHING_BRACKET_PAINTER);
                }
            }
            if (matchingBracketHighlights[0] == null) {
                return;
            }
            
            int matchingBracketOffset = Brackets.findMatchingBracket(chars, offset);
            if (matchingBracketOffset != -1) {
                int start = matchingBracketOffset;
                int end = matchingBracketOffset;
                if (Brackets.isCloseBracket(ch)) {
                    --start;
                } else {
                    ++end;
                }
                matchingBracketHighlights[1] = highlighter.addHighlight(start, end, MATCHING_BRACKET_PAINTER);
            }
        } catch (BadLocationException ex) {
            // Can't happen.
            ex = ex;
        }
    }
}
