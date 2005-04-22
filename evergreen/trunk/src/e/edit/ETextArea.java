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
        // FIXME
        //setMargin(new Insets(4, 4, 4, 1));
    }
    
    /** Returns a fake 'preferred size' if our parent's not tall enough for us to make it as far as the display. */
    public Dimension getPreferredSize() {
        if (getParent().getHeight() <= 0) {
            return new Dimension(0, 0);
        }
        return super.getPreferredSize();
    }
    
    public void setFont(Font font) {
        // Changing font can cause the area around the caret to have moved off-screen.
        // We also have to bear in mind that setFont is called early on during construction, so there may not be a caret!
        int originalCaretPosition = getUnanchoredSelectionExtreme();
        super.setFont(font);
        ensureVisibilityOfOffset(originalCaretPosition);
        
        boolean fixedWidth = GuiUtilities.isFontFixedWidth(font);
        // FIXME
        //setTabSize(fixedWidth ? 8 : 2);
        showRightHandMarginAt(fixedWidth ? 80 : NO_MARGIN);
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
    
    /*
    public void paste() {
        Clipboard clipboard = getToolkit().getSystemClipboard();
        if (isEnabled() == false || clipboard == null) {
            return;
        }
        
        Transferable content = clipboard.getContents(this);
        if (content == null) {
            getToolkit().beep();
            return;
        }
        // FIXME: start CompoundEdit
        //CompoundEdit entireEdit = new CompoundEdit();
        //getUndoManager().addEdit(entireEdit);
            Position position = getDocument().createPosition(getSelectionEnd());
            
            String replacementText = (String) content.getTransferData(DataFlavor.stringFlavor);
            
            // You don't want to paste non-breakable spaces from HTML
            // documentation into your code.
            replacementText = replacementText.replace('\u00a0', ' ');
            
            if (Parameters.getParameter("reformatPastedText", true) && replacementText.indexOf('\n') != -1) {
                int firstLine = getLineOfOffset(getCaretPosition());
                replaceSelection(replacementText);
                int lastLine = getLineOfOffset(getCaretPosition());
                for (int line = firstLine; line < lastLine; line++) {
                    setCaretPosition(getLineStartOffset(line));
                    autoIndent();
                }
            } else {
                replaceSelection(replacementText);
            }
            
            setCaretPosition(position.getOffset());
            // FIXME: end CompoundEdit
            //entireEdit.end();
    }
    */
    
    /** Corrects the indentation of the line with the caret, moving the caret. Returns true if the contents of the current line were changed. */
    public boolean correctIndentation() {
        return getIndenter().correctIndentation(true);
    }
    
    /**
     * Returns the word up to but not past the caret. The intended use is
     * working out what to offer as completions in AutoCompleteAction.
     */
    public String getWordUpToCaret() {
        CharSequence chars = getTextBuffer();
        // FIXME - selection
        int end = getSelectionStart();
        int start = end;
        while (start > 0) {
            char ch = chars.charAt(start - 1);
            if (ch != '_' && Character.isLetterOrDigit(ch) == false) {
                break;
            }
            --start;
        }
        return getTextBuffer().subSequence(start, end).toString();
    }

    /**
     * Overrides getSelectedText to return the empty string instead of null
     * when the selection is empty. Knowing you'll never see null is useful,
     * and the empty string is every bit as good a representation of the
     * empty selection. acme, wily and early versions of Edit using home-grown
     * text components all worked perfectly well in such a world.
     */
    public String getSelectedText() {
        String selection = super.getSelectedText();
        return (selection != null) ? selection : "";
    }
    
    /**
     * Returns the text of the line (without the newline) containing the
     * given offset in the document.
     */
    public String getLineTextAtOffset(int offset) {
        return getLineText(getLineOfOffset(offset));
    }
}
