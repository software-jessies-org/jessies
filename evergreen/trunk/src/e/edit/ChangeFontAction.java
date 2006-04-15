package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import e.util.*;

public abstract class ChangeFontAction extends ETextAction {
    public ChangeFontAction(String fontDescription) {
        super("Use " + fontDescription + " Font");
    }
    
    public abstract Font getFont();
    
    public static Font getConfiguredFont() {
        return getConfiguredFont("font", "verdana", 12);
    }
    
    public static Font getConfiguredFixedFont() {
        return new Font(GuiUtilities.getMonospacedFontName(), Font.PLAIN, 12);
    }
    
    public static Font getConfiguredFont(String parameterPrefix, String defaultFontName, int defaultFontSize) {
        String fontName = Parameters.getParameter(parameterPrefix + ".name", defaultFontName);
        int fontSize = Parameters.getParameter(parameterPrefix + ".size", defaultFontSize);
        return new Font(fontName, Font.PLAIN, fontSize);
    }
    
    /**
     * Returns an appropriate font for the given content.
     * 'Appropriate' basically means the user's configured font,
     * unless the content seems to be such that it would be
     * unreadable except with a fixed font.
     */
    public static Font getAppropriateFontForContent(CharSequence content) {
        return shouldUseFixedFontFor(content) ? getConfiguredFixedFont() : getConfiguredFont();
    }
    
    /**
     * Tests whether we're likely to need a fixed font for the content
     * to be legible. GNU-style indentation and ASCII art are two examples
     * of the kind of thing that requires a fixed font.
     */
    private static boolean shouldUseFixedFontFor(CharSequence content) {
        if (Boolean.getBoolean("fixedFont.default")) {
            return true;
        }
        if (hasMixedTabsAndSpaces(content)) {
            return true;
        }
        if (hasMidLineAsciiArt(content)) {
            return true;
        }
        return false;
    }
    
    /**
     * Tests whether we've got a file with GNU-style lines where you have
     * a mixture of tabs and spaces for indentation.
     * 
     * We exclude the case where it's just a JavaDoc comment that uses a
     * space just to make the stars line up, because we're using this to
     * decide whether or not the file will be unreadable without using a
     * fixed font, and JavaDoc looks fine either way.
     */
    private static boolean hasMixedTabsAndSpaces(CharSequence content) {
        Pattern pattern = Pattern.compile("\\t [^*]", Pattern.MULTILINE);
        return pattern.matcher(content).find();
    }
    
    /**
     * Tries to detect ASCII art heuristically. Multiple spaces which are
     * not part of the indentation probably mean we're dealing with ASCII
     * art. The numbers zero, one and two all exist in the world of computer
     * science, but any more and we're dealing with art.
     */
    private static boolean hasMidLineAsciiArt(CharSequence content) {
        // "\\S\\s{3,}" doesn't work because \s matches newlines even
        // in MULTILINE mode, so we use ' ' instead.
        Pattern pattern = Pattern.compile("\\S {3,}", Pattern.MULTILINE);
        return pattern.matcher(content).find();
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextArea textArea = getFocusedTextArea();
        if (textArea != null) {
            textArea.setFont(getFont());
        }
    }
}
