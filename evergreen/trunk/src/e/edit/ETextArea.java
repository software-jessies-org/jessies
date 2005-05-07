package e.edit;

import java.awt.*;
import java.util.regex.*;
import e.ptextarea.*;
import e.util.*;

/**
 * A text-editing component.
 */
public class ETextArea extends PTextArea {
    public ETextArea() {
        // FIXME: PTextArea doesn't handle this; should we, or should we just
        // force people to add a surrounding JPanel? Swing really made insets
        // and borders way more awkward than they should have been.
        setBorder(new javax.swing.border.EmptyBorder(4, 4, 4, 1));
    }
    
    public void setFont(Font font) {
        super.setFont(font);
        
        // FIXME
        //boolean fixedWidth = GuiUtilities.isFontFixedWidth(font);
        //setTabSize(fixedWidth ? 8 : 2);
    }
    
    /**
     * Sets an appropriate font for this text area's content.
     * Unless you're specifically setting the font to something
     * the user's asked for, this is the only way you should set
     * the font.
     */
    public void setAppropriateFont() {
        setFont(getAppropriateFontForContent());
    }

    /**
     * Returns an appropriate font for this text area's content.
     * 'Appropriate' basically means the user's configured font,
     * unless the content seems to be such that it would be
     * unreadable except with a fixed font.
     */
    public Font getAppropriateFontForContent() {
        return shouldUseFixedFont() ? getConfiguredFixedFont() : getConfiguredFont();
    }
    
    /**
     * Tests whether we're likely to need a fixed font for the content
     * to be legible. GNU-style indentation and ASCII art are two examples
     * of the kind of thing that requires a fixed font.
     */
    private boolean shouldUseFixedFont() {
        if (Boolean.getBoolean("fixedFont.default")) {
            return true;
        }
        String content = getText();
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
    private boolean hasMixedTabsAndSpaces(String content) {
        Pattern pattern = Pattern.compile("\\t [^*]", Pattern.MULTILINE);
        return pattern.matcher(content).find();
    }

    /**
     * Tries to detect ASCII art heuristically. Multiple spaces which are
     * not part of the indentation probably mean we're dealing with ASCII
     * art. The numbers zero, one and two all exist in the world of computer
     * science, but any more and we're dealing with art.
     */
    private boolean hasMidLineAsciiArt(String content) {
        // "\\S\\s{3,}" doesn't work because \s matches newlines even
        // in MULTILINE mode, so we use ' ' instead.
        Pattern pattern = Pattern.compile("\\S {3,}", Pattern.MULTILINE);
        return pattern.matcher(content).find();
    }
    
    public static Font getConfiguredFont() {
        return getConfiguredFont("font", "verdana", 12);
    }
    
    public static Font getConfiguredFixedFont() {
        return getConfiguredFont("fixedFont", "lucida sans typewriter", 12);
    }
    
    public static Font getConfiguredFont(String parameterPrefix, String defaultFontName, int defaultFontSize) {
        String fontName = Parameters.getParameter(parameterPrefix + ".name", defaultFontName);
        int fontSize = Parameters.getParameter(parameterPrefix + ".size", defaultFontSize);
        return new Font(fontName, Font.PLAIN, fontSize);
    }
    
    public String reformatPastedText(String pastedText) {
        return pastedText.replace('\u00a0', ' ');
    }
    
    /**
     * Returns the text of the line (without the newline) containing the
     * given offset in the document.
     */
    public String getLineTextAtOffset(int offset) {
        return getLineText(getLineOfOffset(offset));
    }
}
