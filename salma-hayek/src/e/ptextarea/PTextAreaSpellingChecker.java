package e.ptextarea;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import e.gui.*;
import e.util.*;

public class PTextAreaSpellingChecker implements PTextListener, MenuItemProvider {
    private static final String HIGHLIGHTER_NAME = "PTextAreaSpellingChecker";
    
    private PTextArea component;
    
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
        if (actualRange.isEmpty()) {
            return;
        }
        
        String misspelling = component.getTextBuffer().subSequence(actualRange.getStart(), actualRange.getEnd()).toString();
        String[] suggestions = SpellingChecker.getSharedSpellingCheckerInstance().getSuggestionsFor(misspelling);
        int suggestionCount = 0;
        for (String suggestion : suggestions) {
            if (component.getFileType() != FileType.PLAIN_TEXT) {
                // Since we're editing source, camelCase and underscored_identifiers are more likely than hyphenated words or multiple words.
                suggestion = suggestion.replace('-', '_');
                suggestion = convertMultipleWordsToCamelCase(suggestion);
                // Our attempts to be clever can have the unfortunate consequence of turning a suggestion back into the original misspelling.
                // "FIREWIRE" (in capitals like that) is one example.
                if (suggestion.equals(misspelling)) {
                    continue;
                }
            }
            actions.add(new CorrectSpellingAction(component, suggestion, actualRange.getStart(), actualRange.getEnd()));
            // aspell(1) makes a lot of really bad suggestions.
            // Give up after five suggestions, or if we've seen both camelCase and underscore variants of the misspelled word.
            // Annoyingly for our implementation, the easier-to-spot camelCase variant comes first.
            if (++suggestionCount == 5 || (suggestion.contains("_") && suggestion.replace("_", "").equalsIgnoreCase(misspelling))) {
                break;
            }
        }
        
        if (suggestions.length == 0) {
            actions.add(new NoSuggestionsAction());
        }
        
        actions.add(null);
        actions.add(new AcceptSpellingAction(misspelling, component.getFileType()));
    }
    
    public static class NoSuggestionsAction extends AbstractAction {
        public NoSuggestionsAction() {
            super("(No suggestions)");
            setEnabled(false);
        }
        
        public void actionPerformed(ActionEvent e) {
        }
    }
    
    public static class AcceptSpellingAction extends AbstractAction {
        private final String word;
        private final FileType fileType;
        
        public AcceptSpellingAction(final String word, final FileType fileType) {
            super("Accept '" + word + "'");
            this.word = word;
            this.fileType = fileType;
        }
        
        public void actionPerformed(ActionEvent e) {
            SpellingChecker.getSharedSpellingCheckerInstance().acceptSpelling(word, fileType);
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
        final PTextBuffer buffer = e.getTextBuffer();
        final int offset = e.getOffset();
        final int documentLength = buffer.length();
        
        // Find a plausible place to start before the offset.
        int fromIndex = Math.max(0, offset - 1);
        while (fromIndex > 0 && Character.isWhitespace(buffer.charAt(fromIndex - 1)) == false) {
            fromIndex--;
        }
        
        // Find a plausible place to finish after the end of the range affected by this event.
        int toIndex = e.isRemove() ? offset : offset + e.getLength() + 1;
        toIndex = Math.min(toIndex, documentLength);  // Not past the end.
        toIndex = Math.max(toIndex, fromIndex);  // Not before the start.
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
        new Thread(() -> {
            PTextBuffer buffer = component.getTextBuffer();
            checkSpelling(buffer, 0, buffer.length());
        }, "Spell-Checker Thread").start();
    }
    
    /** Ensures that there are no spelling-related highlights in the given range. */
    private void removeExistingHighlightsForRange(int fromIndex, int toIndex) {
        component.removeHighlights(HIGHLIGHTER_NAME, fromIndex, toIndex);
    }

    public Collection<String> listMisspellings() {
        TreeSet<String> result = new TreeSet<>();
        for (PHighlight highlight : component.getNamedHighlights(HIGHLIGHTER_NAME)) {
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
        List<PHighlight> highlights = component.getNamedHighlightsOverlapping(HIGHLIGHTER_NAME, fromIndex, toIndex);
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
        return Character.isLetter(c) || c == '\'';
    }
    
    private void checkSpelling(PTextBuffer buffer, int fromIndex, int toIndex) {
        int checkCount = 0;
        int misspellingCount = 0;
        
        SpellingChecker spellingChecker = SpellingChecker.getSharedSpellingCheckerInstance();
        
        removeExistingHighlightsForRange(fromIndex, toIndex);
        
        // Breaks the given range up into words, where a changeOfCase or the presence_of_underscores constitutes a word boundary.
        int start = fromIndex;
        int rememberedCase = UNKNOWN_CASE;
        while (start < toIndex) {
            // Skip uncheckable junk.
            while (start < toIndex && Character.isLetterOrDigit(buffer.charAt(start)) == false) {
              start++;
            }
            
            // Skip numbers, including hexadecimal numbers (which we assume start with the decimal digit '0').
            // None of these are (or contain) words to be checked: 1234, 0x1234, 0xdeadbeef, 0x1234fffe.
            char currentChar;
            if (start < toIndex && Character.isDigit(currentChar = buffer.charAt(start))) {
                String allowedDigits = "0123456789";
                ++start;
                if (currentChar == '0' && start < toIndex && buffer.charAt(start) == 'x') {
                    allowedDigits = "0123456789abcdefABCDEF";
                    ++start;
                }
                while (start < toIndex && allowedDigits.indexOf(buffer.charAt(start)) != -1) {
                    ++start;
                }
            }
            
            // Extract a word.
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
            
            // Ignore all-capital words less than 7 characters, on the assumption that they're acronyms.
            // I'm not sure 7 characters isn't too many, but that's what Mac OS' native spelling checker uses.
            if (wordLength < 7 && word.equals(word.toUpperCase())) {
                start = finish;
                continue;
            }
            
            // Ignore 's as a suffix. Should this be done by SpellingChecker instead?
            if (word.endsWith("'s")) {
                word = word.substring(0, word.length() - 2);
            }
            
            //System.err.println(">>" + word + " " + wordLength);
            checkCount++;
            if (spellingChecker.isMisspelledWord(word, component.getFileType())) {
                misspellingCount++;
                //System.err.println("Misspelled word \"" + word + "\"");
                component.addHighlight(new UnderlineHighlight(component, start, finish));
            }
            
            start = finish;
        }
    }
    
    /**
     * A red underline for spelling mistake highlights. On Mac OS, this is a dashed
     * line. Elsewhere, it's a wavy line.
     */
    public static class UnderlineHighlight extends PColoredHighlight {
        // Mac OS uses a dashed underline; MS Windows a wavy one.
        private static final boolean DASHED = GuiUtilities.isMacOs();
        private static final Color COLOR = new Color(255, 0, 0, 160);
        
        // Trying to draw a wiggly line on Linux is horribly slow, alpha blended or not.
        // Therefore we use a buffered painter which pre-generates an alpha-blended image
        // containing an anti-aliased wavy line. This results in approximately a 370x
        // speed improvement on my tests.
        // This was particularly noticable when using a large monitor, with lots of spelling
        // mistakes and the 'find' feature in Evergreen active. Typing would cause a full
        // repaint of the screen, and if the number of wiggly lines was too high, this could easily
        // take several seconds per keypress.
        private BufferedWavyLinePainter linePainter;
        
        public UnderlineHighlight(PTextArea textArea, int startIndex, int endIndex) {
            super(textArea, startIndex, endIndex, COLOR);
            if (!DASHED) {
                linePainter = new BufferedWavyLinePainter(COLOR);
            }
        }
        
        protected boolean paintsToEndOfLine() {
            return false;
        }
        
        @Override
        protected void paintRectangleContents(Graphics2D g, Rectangle r) {
            if (DASHED) {
                paintDashedLine(g, r);
            } else {
                linePainter.paint(g, r.x, r.y + r.height - 3, r.width);
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
    }
    
    // This is abstracted out just so it can be more easily tested (see main function below).
    private static class BufferedWavyLinePainter {
        private BufferedImage img;
        private static final int IMG_WIDTH = 100;
        private static final int IMG_HEIGHT = 3;
        
        public BufferedWavyLinePainter(Color color) {
            img = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setBackground(new Color(0, 0, 0, 0));
            g.clearRect(0, 0, IMG_WIDTH, IMG_HEIGHT);
            g.setColor(color);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int y = 2;
            for (int x = 0; x < IMG_WIDTH; x += 2) {
                g.drawLine(x, y, x + 2, 2 - y);
                y = 2 - y;
            }
        }
        
        private void paint(Graphics2D unclippedG, int xMin, int yMin, int width) {
            Graphics2D g = (Graphics2D) unclippedG.create(xMin, yMin, width, 3);
            for (int x = 0; x < width; x += IMG_WIDTH) {
                g.drawImage(img, x, 0, null);
            }
        }
    }
    
    // The main program here is only to allow speed testing of the wiggly line drawing.
    // Click on the wiggly lines to force a complete redraw, and print-out of timing info.
    public static void main(String[] args) {
        JFrame frame = new JFrame("Wiggly Line Speed Tester");
        JPanel ui = new JPanel(new BorderLayout());
        final BufferedWavyLinePainter painter = new BufferedWavyLinePainter(new Color(255, 102, 102));
        JComponent waverUI = new JComponent() {
            public void paintComponent(Graphics oldGraphics) {
                Graphics2D g = (Graphics2D)oldGraphics;
                Dimension sz = getSize();
                final long startNanos = System.nanoTime();
                for (int y = 10; y < sz.height - 10; y += 10) {
                    painter.paint(g, 10, y, sz.width - 20);
                }
                final long taken = System.nanoTime() - startNanos;
                System.err.println("Paint (" + sz.width + " x " + sz.height + ") took " + taken + " nanoseconds");
            }
        };
        waverUI.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                waverUI.repaint();
            }
        });
        ui.add(waverUI, BorderLayout.CENTER);
        frame.setContentPane(ui);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setSize(new Dimension(1000, 800));
        JFrameUtilities.constrainToScreen(frame);
        JFrameUtilities.setFrameIcon(frame);
        frame.setVisible(true);
    }
}
