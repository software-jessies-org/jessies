package e.edit;

import java.awt.*;
import java.util.*;
import javax.swing.event.*;
import javax.swing.text.*;
import e.util.*;

/**
 * Highlights unmatched brackets in a JTextComponent. Add one of these
 * as a DocumentListener to your JTextComponent.
 */
public class UnmatchedBracketHighlighter implements DocumentListener {
    private static final Highlighter.HighlightPainter PAINTER = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 0, 0, 32));
    
    private JTextComponent textComponent;

    private ArrayList highlights = new ArrayList();
    
    public UnmatchedBracketHighlighter(JTextComponent textComponent) {
        this.textComponent = textComponent;
        updateHighlights();
    }

    private boolean isBracket(char ch) {
        return ch == '(' || ch == ')' || ch == '{' || ch == '}';
    }
    private boolean isIndentationCharacter(char ch) {
        return ch == ' ' || ch == '\t';
    }
    private boolean containsBrackets(String text) {
        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (isBracket(ch)) {
                return true;
            }
            if (isIndentationCharacter(ch)) {
                // We have to consider whitespace significant, too,
                // because we don't just insist that braces match:
                // we also insist that their indentation matches.
                return true;
            }
        }
        return false;
    }
    
    /** Implements the DocumentListener interface so we can watch. */
    public void insertUpdate(DocumentEvent e) {
        try {
            String insertedText = e.getDocument().getText(e.getOffset(), e.getLength());
            if (containsBrackets(insertedText)) {
                updateHighlights();
            }
        } catch (BadLocationException ex) {
            Log.warn("Bad " + e + ".", ex);
        }
    }

    /** Implements the DocumentListener interface so we can watch. */
    public void removeUpdate(DocumentEvent e) {
        updateHighlights();
    }
    
    /** Implements the DocumentListener interface so we can watch. */
    public void changedUpdate(DocumentEvent e) {
        // We don't care about style changes.
    }

    private void removeHighlights() {
        for (int i = 0; i < highlights.size(); ++i) {
            textComponent.getHighlighter().removeHighlight(highlights.get(i));
        }
        highlights = new ArrayList();
    }
    
    /**
     * Rewrites each match of its regular expression with a
     * string of null characters of the same length as the
     * match. This ensures that later document offsets remain
     * as they would have been without the rewrite.
     */
    private static class SameLengthRewriter extends Rewriter {
        public SameLengthRewriter(String regularExpression) {
            super(regularExpression);
        }
        public String replacement() {
            final int length = group(1).length();
            StringBuffer result = new StringBuffer(length);
            result.setLength(length);
            return result.toString();
        }
    }
    private static final Rewriter ESCAPE_CHARACTERS = new SameLengthRewriter("(\\\\.)");
    private static final Rewriter CHARACTER_LITERALS = new SameLengthRewriter("('.')");
    private static final Rewriter STRING_LITERALS = new SameLengthRewriter("(\"([^\\n]*?)\")");
    private static final Rewriter C_PLUS_PLUS_COMMENTS = new SameLengthRewriter("(//[^\\n]*)");
    private static final Rewriter C_COMMENTS = new SameLengthRewriter("(/\\*(?s).*?\\*/)");
    
    public class BracketSite {
        private final String indentation;
        private final char bracketCharacter;
        
        public BracketSite(final String indentation, final char bracketCharacter) {
            this.indentation = indentation;
            this.bracketCharacter = bracketCharacter;
        }
        
        public char getClosingCharacter() {
            if (bracketCharacter == '(') {
                return ')';
            }
            if (bracketCharacter == '{') {
                return '}';
            }
            throw new RuntimeException("invalid opening bracket character " + bracketCharacter);
        }
        
        public boolean equals(Object object) {
            BracketSite rhs = (BracketSite) object;
            return indentation.equals(rhs.indentation) && bracketCharacter == rhs.bracketCharacter;
        }
        
        public int hashCode() {
            return indentation.length() + bracketCharacter;
        }
        
        public boolean isClosedBy(BracketSite closingSite) {
            BracketSite expectedClosingSite = new BracketSite(indentation, getClosingCharacter());
            return closingSite.equals(expectedClosingSite);
        }
    }

    private void updateHighlights() {
        removeHighlights();
        
        String text = textComponent.getText();
        text = ESCAPE_CHARACTERS.rewrite(text);
        text = CHARACTER_LITERALS.rewrite(text);
        text = STRING_LITERALS.rewrite(text);
        text = C_COMMENTS.rewrite(text);
        text = C_PLUS_PLUS_COMMENTS.rewrite(text);

        try {
            Highlighter highlighter = textComponent.getHighlighter();
            Stack indentationStack = new Stack();
            String indentation = "";
            boolean withinIndent = true;
            for (int i = 0; i < text.length(); ++i) {
                char ch = text.charAt(i);
                if (ch == '\n') {
                    indentation = "";
                    withinIndent = true;
                } else if (withinIndent) {
                    if (isIndentationCharacter(ch)) {
                        indentation += ch;
                    } else {
                        withinIndent = false;
                    }
                }
                boolean addHighlight = false;
                if (ch == '(' || ch == '{') {
                    indentationStack.push(new BracketSite(indentation, ch));
                } else if (ch == ')' || ch == '}') {
                    if (indentationStack.empty()) {
                        addHighlight = true;
                    } else {
                        BracketSite openingBracketSite = (BracketSite) indentationStack.peek();
                        if (openingBracketSite.isClosedBy(new BracketSite(indentation, ch)) == false) {
                            addHighlight = true;
                        } else {
                            indentationStack.pop();
                        }
                    }
                }

                if (addHighlight) {
                    highlights.add(highlighter.addHighlight(i, i + 1, PAINTER));
                }
            }
        } catch (BadLocationException ex) {
            Log.warn("Internal error highlighting unmatched brackets.", ex);
        }
    }
}
