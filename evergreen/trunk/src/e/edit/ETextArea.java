package e.edit;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.font.*;
import java.text.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

import e.gui.*;
import e.util.*;

/**
 * A text-editing component.
 */
public class ETextArea extends JTextArea {
    private DocumentUndoFilter undoFilter;
    
    private UndoManager undoManager = new UnlimitedUndoManager();
    
    private JTextComponentSpellingChecker spellingChecker;
    
    private boolean showEightyColumnMargin = false;
    
    private Indenter indenter = new Indenter();
    
    public ETextArea() {
        setBackground(Color.WHITE);
        setCaret(new ECaret());
        setDragEnabled(false);
        setLineWrap(true);
        setMargin(new Insets(4, 4, 4, 1));
        getCaret().addChangeListener(new MatchingBracketHighlighter(this));
        getKeymap().setDefaultAction(new DefaultKeyAction());
    }
    
    /** Returns a fake 'preferred size' if our parent's not tall enough for us to make it as far as the display. */
    public Dimension getPreferredSize() {
        if (getParent().getHeight() <= 0) {
            return new Dimension(0, 0);
        }
        return super.getPreferredSize();
    }
    
    /**
     * Ensures that the UndoManger corresponds to the Document and that the tab
     * size is configured each time the Document changes.
     */
    public void setDocument(Document document) {
        super.setDocument(document);
        if (document != null) {
            if (spellingChecker == null) {
                spellingChecker = new JTextComponentSpellingChecker(this);
            }
            spellingChecker.setDocument(document);
//            if (undoFilter == null) {
//                undoFilter = new DocumentUndoFilter();
//            }
//            undoFilter.setTextComponent(this);
//            undoFilter.addUndoableEditListener(undoManager);
            document.addUndoableEditListener(new UndoableEditListener() {
                public void undoableEditHappened(UndoableEditEvent e) {
                    undoManager.addEdit(e.getEdit());
                }
            });
        }
    }

    /**
     * Sets the content to be the given string. Also
     * sets the caret position to the start of the document.
     */
    public void setText(String text) {
        super.setText(text);
        setCaretPosition(0);
    }
    
    public JTextComponentSpellingChecker getSpellingChecker() {
        return spellingChecker;
    }
    
    public void setFont(Font font) {
        // Changing font can cause the area around the caret to have moved off-screen.
        // We also have to bear in mind that setFont is called early on during construction, so there may not be a caret!
        int originalCaretPosition = (getCaret() != null) ? getCaretPosition() : 0;
        super.setFont(font);
        JTextComponentUtilities.ensureVisibilityOfOffset(this, originalCaretPosition);
        
        boolean fixedWidth = isFontFixedWidth(font);
        setTabSize(fixedWidth ? 8 : 2);
        setShowEightyColumnMargin(fixedWidth);
    }
    
    public static boolean isFontFixedWidth(Font font) {
        // Finds out whether a Font is fixed-width or not. I can't find a more direct way of doing this.
        int maxWidth = 0;
        char[] testChars = "ILMWilmw01".toCharArray();
        for (int i = 0; i < testChars.length; i++) {
            java.awt.geom.Rectangle2D stringBounds = font.getStringBounds(testChars, i, i + 1, DEFAULT_FONT_RENDER_CONTEXT);
            int width = (int) Math.ceil(stringBounds.getWidth());
            if (maxWidth == 0) {
                maxWidth = width;
            } else {
                if (width != maxWidth) {
                    return false;
                }
            }
        }
        return true;
    }
    private static final FontRenderContext DEFAULT_FONT_RENDER_CONTEXT = new FontRenderContext(null, false, false);
    
    public void setShowEightyColumnMargin(boolean showEightyColumnMargin) {
        this.showEightyColumnMargin = showEightyColumnMargin;
    }
    
    public UndoManager getUndoManager() {
        return undoManager;
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

    /**
     * Sets the keymap, mixing in our slight modifications.
     */
    public void setKeymap(Keymap map) {
        super.setKeymap(map);
        
        // Overrides a few DefaultEditorKit editing actions.
        getActionMap().put(DefaultEditorKit.deletePrevCharAction, new BackspaceAction());
        getActionMap().put(DefaultEditorKit.insertTabAction, new InsertTabAction());
        // See also ETextArea.setIndenter.
        
        // Overrides the DefaultEditorKit Home/End actions.
        getActionMap().put(DefaultEditorKit.beginLineAction, new StartOfLineAction(false));
        getActionMap().put(DefaultEditorKit.selectionBeginLineAction, new StartOfLineAction(true));
        getActionMap().put(DefaultEditorKit.endLineAction, new EndOfLineAction(false));
        getActionMap().put(DefaultEditorKit.selectionEndLineAction, new EndOfLineAction(true));
        
        // Overrides the DefaultEditorKit actions for extending the selection or moving the caret to the beginning/end of the current word.
        getActionMap().put(DefaultEditorKit.selectionPreviousWordAction, new PreviousWordAction(true));
        getActionMap().put(DefaultEditorKit.previousWordAction, new PreviousWordAction(false));
        getActionMap().put(DefaultEditorKit.selectionNextWordAction, new NextWordAction(true));
        getActionMap().put(DefaultEditorKit.nextWordAction, new NextWordAction(false));
    }
    
    /**
     * Draws a vertical gray line at the 80 character mark, if we're using a fixed-width font.
     */
    public void paintBorder(Graphics g) {
        super.paintBorder(g);
        if (showEightyColumnMargin) {
            final int x = getMargin().left + g.getFontMetrics().stringWidth("X") * 80;
            g.setColor(Color.LIGHT_GRAY);
            g.setXORMode(Color.WHITE);
            g.drawLine(x, 0, x, getHeight() - 1);
        }
    }
    
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
        CompoundEdit entireEdit = new CompoundEdit();
        getUndoManager().addEdit(entireEdit);
        try {
            String replacementText = (String) content.getTransferData(DataFlavor.stringFlavor);
            if (Parameters.getParameter("reformatPastedText", true) && replacementText.indexOf('\n') != -1) {
                int firstLine = getLineOfOffset(getCaretPosition());
                replaceSelection(replacementText);
                int lastLine = getLineOfOffset(getCaretPosition());
                for (int line = firstLine; line < lastLine; line++) {
                    setCaretPosition(getLineStartOffset(line));
                    correctIndentation(false);
                }
            } else {
                replaceSelection(replacementText);
            }
        } catch (Exception e) {
            getToolkit().beep();
            Log.warn("Exception in paste:" + e.toString());
        } finally {
            entireEdit.end();
        }
    }
    
    /** Returns the indenter in use for this file.
     */
    public Indenter getIndenter() {
        return indenter;
    }
    
    public String getIndentationString() {
        return (String) getDocument().getProperty(Indenter.INDENTATION_PROPERTY);
    }
    
    /**
     * Returns a string corresponding to the spaces and tabs found at the
     * start of the line containing the given offset.
     */
    public String getIndentationOfLineAtOffset(int offset) throws BadLocationException {
        int lineNumber = getLineOfOffset(offset);
        return getIndentationOfLine(lineNumber);
    }
    
    public String getIndentationOfLine(int lineNumber) throws BadLocationException {
        int lineStart = getLineStartOffset(lineNumber);
        int lineEnd = getLineEndOffset(lineNumber);
        Segment currentLine = new Segment();
        getDocument().getText(lineStart, lineEnd - lineStart, currentLine);
        StringBuffer whitespace = new StringBuffer();
        for (char c = currentLine.first(); c != CharacterIterator.DONE; c = currentLine.next()) {
            if (c != ' ' && c != '\t') {
                break;
            }
            whitespace.append(c);
        }
        return whitespace.toString();
    }
    
    /** Corrects the indentation of the line with the caret, moving the caret. Returns true if the contents of the current line were changed. */
    public boolean correctIndentation() {
        return correctIndentation(true);
    }
    
    public void setIndenter(Indenter newIndenter) {
        this.indenter = newIndenter;
    }
    
    public void enableAutoIndent() {
        getActionMap().put(DefaultEditorKit.insertBreakAction, new InsertNewlineAction());
    }
    
    public void autoIndent() {
        correctIndentation(false);
    }
    
    public int getPreviousNonBlankLineNumber(int startLineNumber) {
        try {
            for (int lineNumber = startLineNumber - 1; lineNumber > 0; lineNumber--) {
                if (getLineText(lineNumber).trim().length() != 0) {
                    return lineNumber;
                }
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
        return 0;
    }
    
    /** Corrects the indentation of the line with the caret, optionally moving the caret. Returns true if the contents of the current line were changed. */
    public boolean correctIndentation(boolean shouldMoveCaret) {
        try {
            int position = getCaretPosition();
            int lineNumber = getLineOfOffset(position);

            int offsetIntoLine = position - getLineStartOffset(lineNumber) - getIndentationOfLine(lineNumber).length();

            String whitespace = indenter.getIndentation(this, lineNumber);
            int lineStart = getLineStartOffset(lineNumber);
            int lineLength = getLineEndOffset(lineNumber) - lineStart;
            String originalLine = getLineText(lineNumber);
            String line = originalLine.trim();
            String replacement = whitespace + line;
            //Log.warn("line=@" + originalLine + "@; replacement=@" + replacement + "@");
            boolean lineChanged = (replacement.equals(originalLine) == false);
            if (lineChanged) {
                select(lineStart, lineStart + lineLength);
                replaceSelection(whitespace + line + "\n");
                if (shouldMoveCaret == false) {
                    setCaretPosition(lineStart + whitespace.length() + offsetIntoLine);
                }
            }
            if (shouldMoveCaret) {
                // Move the caret ready to perform the same service to the next line.
                int newCaretPosition = lineStart + whitespace.length() + line.length() + 1;
                newCaretPosition = Math.min(newCaretPosition, getDocument().getLength());
                setCaretPosition(newCaretPosition);
            }
            return lineChanged;
        } catch (BadLocationException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    private String wordSelectionStopChars = " \t\n!\"#%&'()*+,-./:;<=>?@`[\\]^{|}~";
    
    public String getWordSelectionStopChars() {
        return wordSelectionStopChars;
    }
    
    public void setWordSelectionStopChars(String newWordSelectionStopChars) {
        wordSelectionStopChars = newWordSelectionStopChars;
    }
    
    /** Returns the character at the given offset in the document, . */
    public char getCharAt(int offset) throws BadLocationException {
        return getDocument().getText(offset, 1).charAt(0);
    }
    
    /**
     * Returns the word up to but not past the caret. The intended use is
     * working out what to offer as completions in AutoCompleteAction.
     */
    public String getWordUpToCaret() {
        String word = "";
        try {
            int end = getCaretPosition();
            int start = end;
            while (start > 0) {
                char ch = getCharAt(start - 1);
                if (ch != '_' && Character.isLetterOrDigit(ch) == false) {
                    break;
                }
                --start;
            }
            word = getText(start, end - start);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
        return word;
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
    public String getLineTextAtOffset(int offset) throws BadLocationException {
        return getLineText(getLineOfOffset(offset));
    }
    
    /** Returns the text of the given line (without the newline). */
    public String getLineText(int lineNumber) throws BadLocationException {
        int lineStart = getLineStartOffset(lineNumber);
        int lineEnd = getLineEndOffset(lineNumber);
        int length = lineEnd - lineStart;
        if (lineEnd != getDocument().getLength()) {
            length--;
        }
        return (length > 0) ? getText(lineStart, length) : "";
    }

    public void setSelectionColor(Color newColor) {
        // Work around "JTextComponent.setSelectionColor doesn't cause repaint" (review ID 250065; Java Bug Parade id pending).
        super.setSelectionColor(newColor);
        repaint();
    }
}
