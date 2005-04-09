package e.ptextarea;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import e.gui.*;
import e.util.*;

public class PTextAreaSpellingChecker implements PTextListener {
    private PTextArea component;
    
    public static final String KEYWORDS_JCOMPONENT_PROPERTY = "KeywordsHashSetPropertyKey";
    
    public PTextAreaSpellingChecker(PTextArea component) {
        this.component = component;
        initPopUpMenu();
        InstanceTracker.addInstance(this);
        component.getPTextBuffer().addTextListener(this);
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
        
        final Point mousePosition = e.getPoint();
        PCoordinates coordinates = component.getNearestCoordinates(mousePosition);
        final int offset = component.getTextIndex(coordinates);
        
        // FIXME: Ignore this click if the user actually clicked in the right-hand margin.
        
        final Range actualRange = new Range();
        if (isMisspelledWordBetween(offset, offset, actualRange)) {
            EPopupMenu menu = new EPopupMenu();
            String misspelling = component.getPTextBuffer().subSequence(actualRange.start, actualRange.end).toString();
            String[] suggestions = SpellingChecker.getSharedSpellingCheckerInstance().getSuggestionsFor(misspelling);
            for (int i = 0; i < suggestions.length; i++) {
                String suggestion = suggestions[i];
                // Since we're mainly used for editing source, camelCase
                // and underscored_identifiers are more likely than
                // hyphenated words or multiple words.
                suggestion = suggestion.replace('-', '_');
                suggestion = convertMultipleWordsToCamelCase(suggestion);
                menu.add(new CorrectSpellingAction(component, suggestion, actualRange.start, actualRange.end));
            }
            if (suggestions.length == 0) {
                menu.add(new NoSuggestionsAction());
            }
            menu.addSeparator();
            menu.add(new AcceptSpellingAction(misspelling));
            menu.show(component, e.getX(), e.getY());
            e.consume();
        }
    }
    
    public class NoSuggestionsAction extends AbstractAction {
        public NoSuggestionsAction() {
            super("(No suggestions)");
            setEnabled(false);
        }
        
        public void actionPerformed(ActionEvent e) {
        }
    }
    
    public class AcceptSpellingAction extends AbstractAction {
        private final String word;
        
        public AcceptSpellingAction(final String word) {
            super("Accept '" + word + "'");
            this.word = word;
        }
        
        public void actionPerformed(ActionEvent e) {
            SpellingChecker.getSharedSpellingCheckerInstance().acceptSpelling(word);
            Object[] spellingCheckers = InstanceTracker.getInstancesOfClass(PTextAreaSpellingChecker.class);
            for (int i = 0; i < spellingCheckers.length; ++i) {
                ((PTextAreaSpellingChecker) spellingCheckers[i]).checkSpelling();
            }
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
        private PTextArea textArea;
        private String replacement;
        private int startIndex;
        private int endIndex;
        
        public CorrectSpellingAction(PTextArea textArea, String replacement, int startIndex, int endIndex) {
            super("Correct to '" + replacement + "'");
            this.textArea = textArea;
            this.replacement = replacement;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
        
        public void actionPerformed(ActionEvent e) {
            textArea.select(startIndex, endIndex);
            textArea.replaceSelection(replacement);
        }
    }
    
    /** Notification that some text has been inserted into the PText. */
    public void textInserted(PTextEvent event) {
        checkSpelling(event);
    }
    
    /** Notification that some text has been removed from the PText. */
    public void textRemoved(PTextEvent event) {
        checkSpelling(event);
    }
    
    /** Notification that all of the text held within the PText object has been completely replaced. */
    public void textCompletelyReplaced(PTextEvent event) {
        checkSpelling(event);
    }
    
    /**
     * Checks the spelling of the text liable to have been affected by
     * the given DocumentEvent.
     */
    private void checkSpelling(PTextEvent e) {
        PTextBuffer buffer = e.getPTextBuffer();
        final int offset = e.getOffset();
        final int documentLength = buffer.length();
        
        // Find a plausible place to start before the offset.
        int fromIndex = Math.max(0, offset - 1);
        while (fromIndex > 0 && Character.isWhitespace(buffer.charAt(fromIndex - 1)) == false) {
            fromIndex--;
        }
        
        // Find a plausible place to finish after the end of the range affected by this event.
        int toIndex = Math.max(fromIndex, Math.min(documentLength, offset + e.getLength() + 1));
        while (toIndex < documentLength && Character.isWhitespace(buffer.charAt(toIndex)) == false) {
            toIndex++;
        }
        
        //System.err.println("offset: " + offset + " fromIndex: " + fromIndex + " toIndex: " + toIndex + " length: " + documentLength);
        checkSpelling(buffer, fromIndex, toIndex);
    }
    
    /**
     * Checks the spelling of all the text. Runs in a new thread, because
     * it can take a second or more for a large file.
     */
    public void checkSpelling() {
        new Thread(new Runnable() {
            public void run() {
                PTextBuffer buffer = component.getPTextBuffer();
                checkSpelling(buffer, 0, buffer.length());
            }
        }).start();
    }
    
    /** Ensures that there are no spelling-related highlights in the given range. */
    private void removeExistingHighlightsForRange(final int fromIndex, final int toIndex) {
        component.removeHighlights(new PHighlightMatcher() {
            public boolean matches(PHighlight highlight) {
                // FIXME: also check we're removing the right kind of highlight
                return (highlight.getStart().getIndex() < toIndex && highlight.getEnd().getIndex() >= fromIndex);
            }
        });
    }

    public Collection listMisspellings() {
        TreeSet result = new TreeSet();
        List highlights = component.getHighlights();
        for (int i = 0; i < highlights.size(); ++i) {
            PHighlight highlight = (PHighlight) highlights.get(i);
            // FIXME: check we've found the right kind of highlight
            if (true) {
                final int start = highlight.getStart().getIndex();
                final int end = highlight.getEnd().getIndex();
                String misspelling = component.getPTextBuffer().subSequence(start, end).toString();
                result.add(misspelling);
            }
        }
        return result;
    }
    
    public static final class Range {
        public int start;
        public int end;
        public String toString() {
            return "Range[start=" + start + ",end=" + end + "]";
        }
    }
    
    /** Tests whether there's a misspelled word in the given range of offsets. */
    public boolean isMisspelledWordBetween(int fromIndex, int toIndex, Range actualRange) {
        if (toIndex - fromIndex > 20) {
            return false;
        }
        List highlights = component.getHighlights();
        for (int i = 0; i < highlights.size(); ++i) {
            PHighlight highlight = (PHighlight) highlights.get(i);
            // FIXME: check it's one of our highlights.
            if (highlight.getStart().getIndex() <= fromIndex && highlight.getEnd().getIndex() >= toIndex) {
                actualRange.start = Math.min(highlight.getStart().getIndex(), highlight.getEnd().getIndex());
                actualRange.end = Math.max(highlight.getStart().getIndex(), highlight.getEnd().getIndex());
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
    
    private void checkSpelling(PTextBuffer buffer, int fromIndex, int toIndex) {
        long startTime = System.currentTimeMillis();
        int checkCount = 0;
        int misspellingCount = 0;
        
        SpellingChecker spellingChecker = SpellingChecker.getSharedSpellingCheckerInstance();
        
        removeExistingHighlightsForRange(fromIndex, toIndex);
        
        // Breaks the given range up into words, where a changeOfCase or the presence_of_underscores constitutes a word boundary.
        int start = fromIndex;
        int rememberedCase = UNKNOWN_CASE;
        while (start < toIndex) {
            // Skip un-checkable junk.
            while (start < toIndex && Character.isLetter(buffer.charAt(start)) == false) {
              start++;
            }
            
            // Extract a word.
            char currentChar;
            int finish = start;
            while (finish < toIndex && isWordCharacter(currentChar = buffer.charAt(finish))) {
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
            if (finish > start + 1 && buffer.charAt(finish - 1) == '\'') {
                --finish;
            }
            
            final int wordLength = (finish - start);
            
            // Don't bother with words that are too short, or too long.
            // Neither is particularly likely to be a word, and ispell doesn't like long words.
            if (wordLength < 4 || wordLength > 80) {
                start = finish;
                continue;
            }
            
            String word = buffer.subSequence(start, finish).toString();
            
            // Ignore 's as a suffix. Should this be done by SpellingChecker instead?
            if (word.endsWith("'s")) {
                word = word.substring(0, word.length() - 2);
            }
            
            //System.err.println(">>" + word + " " + wordLength);
            checkCount++;
            if (isKeyword(word) == false && spellingChecker.isMisspelledWord(word)) {
                misspellingCount++;
                //System.err.println("Misspelled word '" + word + "'");
                component.addHighlight(new UnderlineHighlight(component, start, finish));
            }
            
            start = finish;
        }
        
        //System.err.println("Took " + (System.currentTimeMillis() - startTime) + "ms to check " + fromIndex + ".." + toIndex + " words:" + checkCount + " misspellings:" + misspellingCount);
    }
    
    /** Tests whether the text component we're checking declares the given word as a keyword in its language. */
    private boolean isKeyword(String word) {
        HashSet keywords = (HashSet) component.getClientProperty(KEYWORDS_JCOMPONENT_PROPERTY);
        if (keywords == null) {
            return false;
        }
        return keywords.contains(word) || keywords.contains(word.toLowerCase());
    }
    
    /**
     * A red underline for spelling mistake highlights. On Mac OS, this is a dashed
     * line. Elsewhere, it's a wavy line.
     */
    public static class UnderlineHighlight extends PColoredHighlight {
        private static final boolean DASHED = GuiUtilities.isMacOs();
        private static final Color COLOR = new Color(255, 0, 0, DASHED ? 160 : 72);
        
        public UnderlineHighlight(PTextArea textArea, int startIndex, int endIndex) {
            super(textArea, startIndex, endIndex, COLOR);
        }
        
        public void paintRectangleContents(Graphics2D g, Rectangle r) {
            if (DASHED) {
                paintDashedLine(g, r);
            } else {
                paintWavyHorizontalLine(g, r.x, r.x + r.width, r.y + r.height - 1);
            }
        }
        
        /*
         * Draws a dashed line by hand. I don't know why I couldn't persuade
         * BasicStroke to draw a 2-pixel wide line, but I couldn't. The "- 1"s
         * here are because the Rectangle is using a half-open range and line
         * drawing uses closed ranges. I've heard the rationale for that, but
         * I still think it causes more trouble than it solves the problem of
         * drawing outlines of filled shapes. Surely the right solution is to
         * have separate fill and outline colors for shape drawing?
         */
        private void paintDashedLine(Graphics2D g, Rectangle r) {
            final int MARK = 3;
            final int SPACE = 1;
            
            final int baseline = r.y + r.height - 1;
            final int stop = r.x + r.width - 1;
            for (int x = r.x; x < stop; x += (MARK + SPACE)) {
                int endX = Math.min(x + MARK - 1, stop);
                g.drawLine(x, baseline, endX, baseline);
                g.drawLine(x, baseline - 1, endX, baseline - 1);
            }
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
