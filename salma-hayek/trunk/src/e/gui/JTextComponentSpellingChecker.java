package e.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import e.util.*;

public class JTextComponentSpellingChecker implements DocumentListener {
    private JTextComponent component;
    
    private Document document;
    
    public static final String KEYWORDS_DOCUMENT_PROPERTY = "KeywordsHashSetPropertyKey";
    
    public JTextComponentSpellingChecker(JTextComponent component) {
        this.component = component;
        initPopUpMenu();
    }
    
    private void initPopUpMenu() {
        component.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowSpellingMenu(e);
            }
            
            public void mouseReleased(MouseEvent e) {
                maybeShowSpellingMenu(e);
            }
        });
    }
    
    /**
     * Pops up a menu containing suggested corrections to
     * the spelling mistake at the coordinates given by the
     * MouseEvent.
     * 
     * If there is no mistake, no menu is shown.
     * 
     * The MouseEvent is consumed if a menu is shown, so
     * other MouseListeners checking isPopupTrigger can
     * also check isConsumed.
     */
    private void maybeShowSpellingMenu(MouseEvent e) {
        if (e.isPopupTrigger() == false) {
            return;
        }
        
        try {
            final Range actualRange = new Range();
            final int offset = component.viewToModel(e.getPoint());
            if (isMisspelledWordBetween(offset, offset, actualRange)) {
                EPopupMenu menu = new EPopupMenu();
                String misspelling = document.getText(actualRange.start, actualRange.end - actualRange.start);
                //menuItems.add(new AcceptSpellingAction(misspelling));
                String[] suggestions = SpellingChecker.getSharedSpellingCheckerInstance().getSuggestionsFor(misspelling);
                for (int i = 0; i < suggestions.length; i++) {
                    String suggestion = suggestions[i];
                    // Since we're mainly used for editing source, camelCase
                    // and underscored_identifiers are more likely than
                    // hyphenated words or multiple words.
                    suggestion = suggestion.replace('-', '_');
                    suggestion = convertMultipleWordsToCamelCase(suggestion);
                    menu.add(new CorrectSpellingAction(document, suggestion, actualRange.start, actualRange.end));
                }
                menu.show(component, e.getX(), e.getY());
                e.consume();
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }
    
    private String convertMultipleWordsToCamelCase(String original) {
        StringBuffer result = new StringBuffer(original);
        for (int i = 0; i < result.length(); ++i) {
            if (result.charAt(i) == ' ' && i < result.length() - 1) {
                result.deleteCharAt(i);
                result.setCharAt(i, Character.toUpperCase(result.charAt(i)));
            }
        }
        return result.toString();
    }
    
    private static class CorrectSpellingAction extends AbstractAction {
        private Document document;
        private String replacement;
        private int startIndex;
        private int endIndex;
        
        public CorrectSpellingAction(Document document, String replacement, int startIndex, int endIndex) {
            super("Correct to '" + replacement + "'");
            this.document = document;
            this.replacement = replacement;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
        
        public void actionPerformed(ActionEvent e) {
            if (document != null) {
                try {
                    if (document instanceof AbstractDocument) {
                        ((AbstractDocument) document).replace(startIndex, endIndex - startIndex, replacement, null);
                    } else {
                        document.remove(startIndex, endIndex - startIndex);
                        document.insertString(startIndex, replacement, null);
                    }
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
    public void setDocument(Document newDocument) {
        Document oldDocument = document;
        if (oldDocument != null) {
            oldDocument.removeDocumentListener(this);
        }
        
        document = newDocument;
        document.addDocumentListener(this);
    }
    
    public void insertUpdate(DocumentEvent e) {
        checkSpelling(e);
    }
    
    public void removeUpdate(DocumentEvent e) {
        checkSpelling(e);
    }

    public void changedUpdate(DocumentEvent e) {
        checkSpelling(e);
    }

    /**
     * Checks the spelling of the text liable to have been affected by
     * the given DocumentEvent.
     */
    private void checkSpelling(DocumentEvent e) {
        try {
            final int offset = e.getOffset();
            final int documentLength = document.getLength();
            String segment = document.getText(0, documentLength);
            
            // Find a plausible place to start before the offset.
            int fromIndex = Math.max(0, offset - 1);
            while (fromIndex > 0 && Character.isWhitespace(segment.charAt(fromIndex - 1)) == false) {
                fromIndex--;
            }

            // Find a plausible place to finish after the end of the range affected by this event.
            int toIndex = Math.max(fromIndex, Math.min(documentLength, offset + e.getLength() + 1));
            while (toIndex < documentLength && Character.isWhitespace(segment.charAt(toIndex)) == false) {
                toIndex++;
            }
            
            //System.err.println("offset: " + offset + " fromIndex: " + fromIndex + " toIndex: " + toIndex + " length: " + documentLength);
            checkSpelling(document, segment, fromIndex, toIndex);
        } catch (Exception ex) {
            // Spelling is an optional extra, and definitely not worth losing data over.
            ex.printStackTrace();
        }
    }
    
    /** Checks the spelling of all the text. */
    public void checkSpelling() {
        try {
            final int fromIndex = 0;
            final int toIndex = document.getLength();
            String segment = document.getText(fromIndex, toIndex);
            checkSpelling(document, segment, fromIndex, toIndex);
        } catch (Exception ex) {
            // Spelling is an optional extra, and definitely not worth losing data over.
            ex.printStackTrace();
        }
    }
    
    private void dumpHighlights() {
        Highlighter highlighter = component.getHighlighter();
        Highlighter.Highlight[] highlights = highlighter.getHighlights();
        for (int i = 0; i < highlights.length; ++i) {
            Highlighter.Highlight highlight = highlights[i];
            System.err.println("  " + i + " : " + highlight.getStartOffset() + ".." + highlight.getEndOffset());
        }
    }
    
    /** Ensures that there are no spelling-related highlights in the given range. */
    private void removeExistingHighlightsForRange(int fromIndex, int toIndex) {
        Highlighter highlighter = component.getHighlighter();
        Highlighter.Highlight[] highlights = highlighter.getHighlights();
        
        //System.err.println(fromIndex + ".." + toIndex + " (document length " + document.getLength() + ")");
        for (int i = 0; i < highlights.length; ++i) {
            Highlighter.Highlight highlight = highlights[i];
            if (highlight.getPainter() == PAINTER && highlight.getStartOffset() >= fromIndex && highlight.getEndOffset() <= toIndex) {
                highlighter.removeHighlight(highlight);
            }
        }
    }

    public Collection listMisspellings() {
        Highlighter highlighter = component.getHighlighter();
        Highlighter.Highlight[] highlights = highlighter.getHighlights();
        
        TreeSet result = new TreeSet();
        for (int i = 0; i < highlights.length; i++) {
            Highlighter.Highlight highlight = highlights[i];
            if (highlight.getPainter() == PAINTER) {
                final int start = highlight.getStartOffset();
                final int end = highlight.getEndOffset();
                try {
                    String misspelling = document.getText(start, (end - start));
                    result.add(misspelling);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return result;
    }
    
    public static final class Range {
        public int start;
        public int end;
    }
    
    /** Tests whether there's a misspelled word in the given range of offsets. */
    public boolean isMisspelledWordBetween(int fromIndex, int toIndex, Range actualRange) {
        if (toIndex - fromIndex > 20) {
            return false;
        }
        Highlighter highlighter = component.getHighlighter();
        Highlighter.Highlight[] highlights = highlighter.getHighlights();
        
        for (int i = 0; i < highlights.length; i++) {
            Highlighter.Highlight highlight = highlights[i];
            if (highlight.getPainter() == PAINTER && highlight.getStartOffset() <= fromIndex && highlight.getEndOffset() >= toIndex) {
                actualRange.start = Math.min(highlight.getStartOffset(), highlight.getEndOffset());
                actualRange.end = Math.max(highlight.getStartOffset(), highlight.getEndOffset());
                return true;
            }
        }
        return false;
    }
    
    private static final int UNKNOWN_CASE = 0;
    private static final int UPPER_CASE = 1;
    private static final int LOWER_CASE = 2;
    
    /**
     * Tests whether a character should be allowed to be a continuation of the current word.
     * We include all alphabetic characters, no digits, and no '_' because we
     * assume that an underscore marks a break in a compound identifier (and we want to
     * present the spelling checker with individual words).
     * We also allow apostrophe, because "doesn't" and the like
     * constitute words in comments. This could cause trouble for Ada and VHDL source, but
     * I'll worry about them when I have reason to.
     */
    private final boolean isWordCharacter(char c) {
        return Character.isLetter(c) || c == '\'';
    }
    
    private void checkSpelling(Document document, String segment, int fromIndex, int toIndex) {
        long startTime = System.currentTimeMillis();
        int checkCount = 0;
        int misspellingCount = 0;
        
        SpellingChecker spellingChecker = SpellingChecker.getSharedSpellingCheckerInstance();
        Highlighter highlighter = component.getHighlighter();
        
        removeExistingHighlightsForRange(fromIndex, toIndex);
        
        // Breaks the given range up into words, where a changeOfCase or the presence_of_underscores constitutes a word boundary.
        int start = fromIndex;
        int rememberedCase = UNKNOWN_CASE;
        while (start < toIndex) {
            // Skip un-checkable junk.
            while (start < toIndex && Character.isLetter(segment.charAt(start)) == false) {
              start++;
            }
            
            // Extract a word.
            char currentChar;
            int finish = start;
            while (finish < toIndex && isWordCharacter(currentChar = segment.charAt(finish))) {
                int previousCase = rememberedCase;
                int thisCase = (Character.isLowerCase(currentChar) ? LOWER_CASE : (Character.isUpperCase(currentChar) ? UPPER_CASE : UNKNOWN_CASE));
                rememberedCase = thisCase;
                
                // Spot CamelCase changesLikeThese.
                if (thisCase == UPPER_CASE && previousCase == LOWER_CASE) {
                    break;
                }
                
                // Spot acronyms, such as IOException and URLConnection.
                if (thisCase == LOWER_CASE && previousCase == UPPER_CASE && (finish - start) > 1) {
                    finish--;
                    break;
                }
                
                finish++;
            }
            
            // Don't include a final ', because it's not an apostrophe.
            if (finish > start + 1 && segment.charAt(finish - 1) == '\'') {
                --finish;
            }
            
            final int wordLength = (finish - start);
            
            // Don't bother with words that are too short, or too long.
            // Neither is particularly likely to be a word, and ispell doesn't like long words.
            if (wordLength < 4 || wordLength > 80) {
                start = finish;
                continue;
            }
            
            String word = segment.substring(start, finish);
            
            // Ignore 's as a suffix. Should this be done by SpellingChecker instead?
            if (word.endsWith("'s")) {
                word = word.substring(0, word.length() - 2);
            }
            
            //System.err.println(">>" + word + " " + wordLength);
            checkCount++;
            if (isKeyword(word) == false && spellingChecker.isMisspelledWord(word)) {
                misspellingCount++;
                //System.err.println("Misspelled word '" + word + "'");
                try {
                    highlighter.addHighlight(start, finish, PAINTER);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
            
            start = finish;
        }
        
        //System.err.println("Took " + (System.currentTimeMillis() - startTime) + "ms to check " + fromIndex + ".." + toIndex + " words:" + checkCount + " misspellings:" + misspellingCount);
    }
    
    /** Tests whether the Document we're checking declares the given word as a keyword in its language. */
    private boolean isKeyword(String word) {
        HashSet keywords = (HashSet) document.getProperty(KEYWORDS_DOCUMENT_PROPERTY);
        if (keywords == null) {
            return false;
        }
        return keywords.contains(word) || keywords.contains(word.toLowerCase());
    }
    
    /** Paints the misspelled word highlights. */
    private static final LayeredHighlighter.LayerPainter PAINTER = new UnderlineHighlightPainter();
    
    /**
     * A red underline for spelling mistake highlights. On Mac OS, this is a dashed
     * line. Elsewhere, it's a wavy line.
     */
    public static class UnderlineHighlightPainter extends LayeredHighlighter.LayerPainter {
        private static final boolean DASHED = GuiUtilities.isMacOs();
        private static final Color COLOR = new Color(255, 0, 0, DASHED ? 128 : 72);
        private static final Stroke DASHED_STROKE = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { 2.0f, 3.0f }, 0.0f);
        
        public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
            // Do nothing: this method will never be called
        }
        
        public Shape paintLayer(Graphics g, int offs0, int offs1, Shape shape, JTextComponent c, View view) {
            if ((offs0 == view.getStartOffset() && offs1 == view.getEndOffset()) == false) {
                try {
                    shape = view.modelToView(offs0, Position.Bias.Forward, offs1, Position.Bias.Backward, shape);
                } catch (BadLocationException ex) {
                    return null;
                }
            }
            g.setColor(COLOR);
            Rectangle r = shape.getBounds();
            if (DASHED) {
                paintDashedLine((Graphics2D) g, r);
            } else {
                paintWavyHorizontalLine(g, r.x, r.x + r.width, r.y + r.height - 1);
            }
            return r;
        }
        
        private void paintDashedLine(Graphics2D g, Rectangle r) {
            int baseline = r.y + r.height - 1;
            r.y += 2; r.height -= 2;
            
            Stroke originalStroke = g.getStroke();
            g.setStroke(DASHED_STROKE);
            g.drawLine(r.x, baseline, r.x + r.width, baseline);
            g.setStroke(originalStroke);
        }
        
        private void paintWavyHorizontalLine(Graphics g, int x1, int x2, int y) {
            int x = Math.min(x1, x2);
            int end = Math.max(x1, x2);
            int yOff = 1;
            while (x < end) {
                g.drawLine(x, y + yOff, Math.min(end, x + 2), y - yOff);
                x += 2;
                yOff = -yOff;
            }
        }
    }
}
