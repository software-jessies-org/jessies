package e.gui;

import java.awt.*;
import java.util.*;
import javax.swing.event.*;
import javax.swing.text.*;
import e.util.SpellingChecker;

public class JTextComponentSpellingChecker implements DocumentListener {
    private JTextComponent component;
    
    private Document document;
    
    public static final String KEYWORDS_DOCUMENT_PROPERTY = "KeywordsHashSetPropertyKey";
    
    public JTextComponentSpellingChecker(JTextComponent component) {
        this.component = component;
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
            if (highlight.getPainter() == misspelledWordHighlightPainter && highlight.getStartOffset() >= fromIndex && highlight.getEndOffset() <= toIndex) {
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
            if (highlight.getPainter() == misspelledWordHighlightPainter) {
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
            if (highlight.getPainter() == misspelledWordHighlightPainter && highlight.getStartOffset() <= fromIndex && highlight.getEndOffset() >= toIndex) {
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
                    highlighter.addHighlight(start, finish, misspelledWordHighlightPainter);
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
    private Highlighter.HighlightPainter misspelledWordHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 0, 0, 32));
}
