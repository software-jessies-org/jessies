package e.ptextarea;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import e.gui.*;
import e.util.*;

public class PTextAreaSpellingChecker implements PTextListener, MenuItemProvider {
    private PTextArea component;
    
    private static final String HIGHLIGHTER_NAME = "PTextAreaSpellingChecker";
    public static final String SPELLING_EXCEPTIONS_PROPERTY = "org.jessies.e.ptextarea.SpellingExceptionsHashSetProperty";
    
    public PTextAreaSpellingChecker(PTextArea component) {
        this.component = component;
        initPopUpMenu();
        InstanceTracker.addInstance(this);
        component.getTextBuffer().addTextListener(this);
    }
    
    private void initPopUpMenu() {
        component.getPopupMenu().addMenuItemProvider(this);
    }
    
    public void provideMenuItems(MouseEvent e, Collection<Action> actions) {
        final Point mousePosition = e.getPoint();
        PCoordinates coordinates = component.getNearestCoordinates(mousePosition);
        final int offset = component.getTextIndex(coordinates);
        
        // FIXME: Ignore this click if the user actually clicked in the right-hand margin.
        
        final Range actualRange = rangeOfMisspelledWordBetween(offset, offset);
        if (actualRange.isEmpty() == false) {
            String misspelling = component.getTextBuffer().subSequence(actualRange.getStart(), actualRange.getEnd()).toString();
            String[] suggestions = SpellingChecker.getSharedSpellingCheckerInstance().getSuggestionsFor(misspelling);
            for (String suggestion : suggestions) {
                // Since we're mainly used for editing source, camelCase
                // and underscored_identifiers are more likely than
                // hyphenated words or multiple words.
                suggestion = suggestion.replace('-', '_');
                suggestion = convertMultipleWordsToCamelCase(suggestion);
                actions.add(new CorrectSpellingAction(component, suggestion, actualRange.getStart(), actualRange.getEnd()));
            }
            if (suggestions.length == 0) {
                actions.add(new NoSuggestionsAction());
            }
            actions.add(null);
            actions.add(new AcceptSpellingAction(misspelling));
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
            for (PTextAreaSpellingChecker spellingChecker : InstanceTracker.getInstancesOfClass(PTextAreaSpellingChecker.class)) {
                spellingChecker.checkSpelling();
            }
        }
    }
    
    private String convertMultipleWordsToCamelCase(String original) {
        StringBuilder result = new StringBuilder(original);
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
            textArea.replaceRange(replacement, startIndex, endIndex);
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
        PTextBuffer buffer = e.getTextBuffer();
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
                PTextBuffer buffer = component.getTextBuffer();
                checkSpelling(buffer, 0, buffer.length());
            }
        }, "Spell-Checker Thread").start();
    }
    
    /** Ensures that there are no spelling-related highlights in the given range. */
    private void removeExistingHighlightsForRange(int fromIndex, int toIndex) {
        component.removeHighlights(HIGHLIGHTER_NAME, fromIndex, toIndex);
    }

    public Collection listMisspellings() {
        TreeSet<String> result = new TreeSet<String>();
        for (PHighlight highlight : component.getHighlights(HIGHLIGHTER_NAME)) {
            final int start = highlight.getStartIndex();
            final int end = highlight.getEndIndex();
            String misspelling = component.getTextBuffer().subSequence(start, end).toString();
            result.add(misspelling);
        }
        return result;
    }
    
    /** Tests whether there's a misspelled word in the given range of offsets. */
    public Range rangeOfMisspelledWordBetween(int fromIndex, int toIndex) {
        if (toIndex - fromIndex > 20) {
            return Range.NULL_RANGE;
        }
        List<PHighlight> highlights = component.getHighlights(HIGHLIGHTER_NAME, fromIndex, toIndex);
        if (highlights.size() > 0) {
            return new Range(highlights.get(0).getStartIndex(), highlights.get(0).getEndIndex());
        } else {
            return Range.NULL_RANGE;
        }
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
        return Character.isLetterOrDigit(c) || c == '\'';
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
        int lineStart = start;
        while (start < toIndex) {
            // Skip un-checkable junk.
            int spaceStart = start;
            while (start < toIndex && Character.isLetterOrDigit(buffer.charAt(start)) == false) {
                char ch = buffer.charAt(start);
                if (ch == '\n') {
                    if (spaceStart != start && spaceStart != lineStart) {
                        component.addHighlight(new UnderlineHighlight(component, spaceStart, start));
                    }
                    spaceStart = lineStart = start + 1;
                } else if (Character.isWhitespace(ch) == false) {
                    spaceStart = start + 1;
                }
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
            boolean isNumber = word.matches("(?:0x[0-9A-Fa-f]+|[1-9][0-9]*)U?L?L?");
            if (isException(word) == false && isNumber == false && spellingChecker.isMisspelledWord(word)) {
                misspellingCount++;
                //System.err.println("Misspelled word '" + word + "'");
                component.addHighlight(new UnderlineHighlight(component, start, finish));
            }
            
            start = finish;
        }
        
        //System.err.println("Took " + (System.currentTimeMillis() - startTime) + "ms to check " + fromIndex + ".." + toIndex + " words:" + checkCount + " misspellings:" + misspellingCount);
    }
    
    /**
     * Tests whether the text component we're checking declares the given word
     * as a spelling exception in its language.
     */
    private boolean isException(String word) {
        HashSet exceptions = HashSet.class.cast(component.getClientProperty(SPELLING_EXCEPTIONS_PROPERTY));
        if (exceptions == null) {
            return false;
        }
        String lowerCaseWord = word.toLowerCase();
        return exceptions.contains(word) || exceptions.contains(lowerCaseWord);
    }
    
    /**
     * A red underline for spelling mistake highlights. On Mac OS, this is a dashed
     * line. Elsewhere, it's a wavy line.
     */
    public static class UnderlineHighlight extends PColoredHighlight {
        // Mac OS uses a dashed underline; MS Windows a wavy one.
        private static final boolean DASHED = GuiUtilities.isMacOs();
        
        // On Linux, painting with an alpha color is *really* slow, so use an
        // approximate pink if we're not on Mac OS.
        private static final Color COLOR = DASHED ? new Color(255, 0, 0, 160) : new Color(255, 102, 102);
        
        public UnderlineHighlight(PTextArea textArea, int startIndex, int endIndex) {
            super(textArea, startIndex, endIndex, COLOR);
        }
    
        protected boolean paintsToEndOfLine() {
            return false;
        }
        
        public void paintRectangleContents(Graphics2D g, Rectangle r) {
            if (DASHED) {
                paintDashedLine(g, r);
            } else {
                paintWavyHorizontalLine(g, r.x, r.x + r.width, r.y + r.height - 1);
            }
        }
        
        public String getHighlighterName() {
            return HIGHLIGHTER_NAME;
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
