package e.edit;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.font.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

import e.gui.JTextComponentSpellingChecker;
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
    
    /** All ETextArea instances share the same keymap. */
    protected static Keymap sharedKeymap;
    
    private static final String KEYMAP_NAME = "ETextWindowKeymap";
    
    private static final FindAction FIND_ACTION = new FindAction();
    
    private static final Action[] KEYMAP_ACTIONS = {
        new BackspaceAction(),
        
        new BuildAction(),
        new CloseWindowAction(),
        new CorrectIndentationAction(),
        
        /*
         * We need to offer these because the Motif LAF binds them to
         * the COPY, CUT and PASTE keys on Sun keyboards, but we want
         * them bound to C-C, C-X and C-V on all keyboards. This is,
         * after all, the 21st century.
         */
        new CopyAction(),
        new CutAction(),
        new PasteAction(),
        
        new EndOfLineAction(true),
        new EndOfLineAction(false),
        FIND_ACTION,
        new FindAndReplaceAction(),
        new FindNextAction(),
        new FindPreviousAction(),
        new GotoAction(),
        new InsertBracePairAction(),
        new InsertInterfaceAction(),
        new InsertNewlineAction(),
        new InsertTabAction(),
        new KillErrorsAction(),
        new OpenAction(),
        new RedoAction(),
        new SaveAction(),
        new StartOfLineAction(true),
        new StartOfLineAction(false),
        new UndoAction()
    };
    
    private static JTextComponent.KeyBinding[] keyBindings;
    
    public static JTextComponent.KeyBinding makeShiftCommandKeyBinding(int virtualKey, String actionName) {
        JTextComponent.KeyBinding result = new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(virtualKey, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK), actionName);
        if (result == null) {
            throw new IllegalArgumentException("virtualKey=" + virtualKey + "; actionName=\"" + actionName + "\"");
        }
        return result;
    }
    
    public static JTextComponent.KeyBinding makeCommandKeyBinding(int virtualKey, String actionName) {
        JTextComponent.KeyBinding result = new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(virtualKey, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), actionName);
        if (result == null) {
            throw new IllegalArgumentException("virtualKey=" + virtualKey + "; actionName=\"" + actionName + "\"");
        }
        return result;
    }
    
    public static JTextComponent.KeyBinding makeKeyBinding(String key, String actionName) {
        JTextComponent.KeyBinding result = new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(key), actionName);
        if (result == null) {
            throw new IllegalArgumentException("key='" + key + "'; actionName=\"" + actionName + "\"");
        }
        return result;
    }
    
    public static JTextComponent.KeyBinding makeCharacterKeyBinding(Character character, String actionName) {
        JTextComponent.KeyBinding result = new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(character, 0), actionName);
        if (result == null) {
            throw new IllegalArgumentException("character='" + character + "'; actionName=\"" + actionName + "\"");
        }
        return result;
    }
    
    static {
        ArrayList list = new ArrayList();
        list.add(makeCommandKeyBinding(KeyEvent.VK_B, BuildAction.ACTION_NAME));
        list.add(makeCommandKeyBinding(KeyEvent.VK_C, CopyAction.ACTION_NAME));
        list.add(makeCommandKeyBinding(KeyEvent.VK_D, FindPreviousAction.ACTION_NAME));
        list.add(makeCommandKeyBinding(KeyEvent.VK_F, FindAction.ACTION_NAME));
        list.add(makeCommandKeyBinding(KeyEvent.VK_G, FindNextAction.ACTION_NAME));
        list.add(makeCommandKeyBinding(KeyEvent.VK_I, CorrectIndentationAction.ACTION_NAME));
        list.add(makeCommandKeyBinding(KeyEvent.VK_J, InsertInterfaceAction.ACTION_NAME));
        list.add(makeCommandKeyBinding(KeyEvent.VK_K, KillErrorsAction.ACTION_NAME));
        list.add(makeCommandKeyBinding(KeyEvent.VK_L, GotoAction.ACTION_NAME));
        list.add(makeCommandKeyBinding(KeyEvent.VK_O, OpenAction.ACTION_NAME));
        list.add(makeCommandKeyBinding(KeyEvent.VK_R, FindAndReplaceAction.ACTION_NAME));
        list.add(makeCommandKeyBinding(KeyEvent.VK_S, SaveAction.ACTION_NAME));
        list.add(makeCommandKeyBinding(KeyEvent.VK_V, PasteAction.ACTION_NAME));
        list.add(makeCommandKeyBinding(KeyEvent.VK_W, CloseWindowAction.ACTION_NAME));
        list.add(makeCommandKeyBinding(KeyEvent.VK_X, CutAction.ACTION_NAME));
        list.add(makeCommandKeyBinding(KeyEvent.VK_Z, UndoAction.ACTION_NAME));
        list.add(makeShiftCommandKeyBinding(KeyEvent.VK_Z, RedoAction.ACTION_NAME));
        //list.add(makeKeyBinding("ctrl BRACELEFT", InsertBracePairAction.ACTION_NAME));
        list.add(makeKeyBinding("TAB", InsertTabAction.ACTION_NAME));
        
        // Unfortunately, Java 1.5.0 beta1 has changed back from \010.
        // I wish they'd just make up their minds!
        if (System.getProperty("java.version").indexOf("1.5") != -1) {
            list.add(makeKeyBinding("BACK_SPACE", BackspaceAction.ACTION_NAME));
        } else {
            list.add(makeKeyBinding("typed \010", BackspaceAction.ACTION_NAME));
        }
        
        list.add(makeKeyBinding("ENTER", InsertNewlineAction.ACTION_NAME));
        list.add(makeKeyBinding("END", EndOfLineAction.ACTION_NAME));
        list.add(makeKeyBinding("shift END", EndOfLineAction.SHIFT_ACTION_NAME));
        list.add(makeKeyBinding("HOME", StartOfLineAction.ACTION_NAME));
        list.add(makeKeyBinding("shift HOME", StartOfLineAction.SHIFT_ACTION_NAME));
        
        // Old-style DOS keyboard copy/cut/paste equivalents for martind.
        list.add(makeKeyBinding("control INSERT", CopyAction.ACTION_NAME));
        list.add(makeKeyBinding("shift INSERT", PasteAction.ACTION_NAME));
        list.add(makeKeyBinding("shift DELETE", CutAction.ACTION_NAME));
        
        keyBindings = (JTextComponent.KeyBinding[]) list.toArray(new JTextComponent.KeyBinding[list.size()]);
    }
    
    public ETextArea() {
        setBackground(Color.WHITE);
        setCaret(new ECaret());
        setDragEnabled(false);
        setLineWrap(true);
        setMargin(new Insets(4, 4, 4, 1));
        initColors();
    }
    
    public void find(String regularExpression) {
        FIND_ACTION.findInText((ETextWindow) SwingUtilities.getAncestorOfClass(ETextWindow.class, this), regularExpression);
    }
    
    public void initColors() {
        Color selectionBackground = Color.getColor("selection.backgroundColor");
        if (selectionBackground != null) {
            setSelectionColor(selectionBackground);
        }
        Color selectionForeground = Color.getColor("selection.foregroundColor");
        if (selectionForeground != null) {
            setSelectedTextColor(selectionForeground);
        }
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
     * Sets the content to be the given string. Also ensures that
     * text is displayed in a suitable font for the content, and
     * sets the caret position to the start of the document.
     */
    public void setText(String text) {
        super.setText(text);
        setAppropriateFont();
        setCaretPosition(0);
    }
    
    public JTextComponentSpellingChecker getSpellingChecker() {
        return spellingChecker;
    }
    
    public void ensureVisibilityOfOffset(int offset) {
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
        if (viewport == null) {
            return;
        }
        int height = viewport.getExtentSize().height;
        try {
            Rectangle viewRectangle = modelToView(offset);
            if (viewRectangle == null) {
                return;
            }
            int y = viewRectangle.y - height/2;
            y = Math.max(0, y);
            y = Math.min(y, getHeight() - height);
            viewport.setViewPosition(new Point(0, y));
        } catch (javax.swing.text.BadLocationException ex) {
            ex.printStackTrace();
        }
    }
    
    public void setFont(Font font) {
        // Changing font can cause the area around the caret to have moved off-screen.
        // We also have to bear in mind that setFont is called early on during construction, so there may not be a caret!
        int originalCaretPosition = (getCaret() != null) ? getCaretPosition() : 0;
        super.setFont(font);
        ensureVisibilityOfOffset(originalCaretPosition);
        
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
     * Returns an appropriate font for this text area's content.
     * 'Appropriate' basically means the user's configured font,
     * unless the content seems to use a mixture of tabs and spaces
     * for indentation, in which case, a fixed font is deemed
     * appropriate regardless of the user's default preference.
     */
    public Font getAppropriateFontForContent() {
        boolean fixed = Boolean.getBoolean("fixedFont.default");
        // FIXME: we should really hand this work out to an IndentationAnalyzer
        // that also works out a suitable default indent string.
        String content = getText();
        if (content.indexOf("\t ") != -1) {
            fixed = true;
        }
        return fixed ? getConfiguredFixedFont() : getConfiguredFont();
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
     * Sets the keymap, adding in our extra bindings.
     */
    public void setKeymap(Keymap map) {
        if (map == null) {
            //Log.warn("Setting null keymap.");
            super.setKeymap(null);
            sharedKeymap = null;
            return;
        }
        if (getKeymap() == null) {
            if (sharedKeymap == null) {
                // Switch keymaps & add extra bindings.
                removeKeymap(KEYMAP_NAME);
                sharedKeymap = addKeymap(KEYMAP_NAME, map);
                loadKeymap(sharedKeymap, keyBindings, KEYMAP_ACTIONS);
            }
            map = sharedKeymap;
        }
        //Log.warn("Setting keymap " + map);
        super.setKeymap(map);
        
        // Overrides the DefaultEditorKit actions for extending the selection or moving the caret to the beginning/end of the current word.
        getActionMap().put(DefaultEditorKit.selectionPreviousWordAction, new PreviousWordAction(true));
        getActionMap().put(DefaultEditorKit.previousWordAction, new PreviousWordAction(false));
        getActionMap().put(DefaultEditorKit.selectionNextWordAction, new NextWordAction(true));
        getActionMap().put(DefaultEditorKit.nextWordAction, new NextWordAction(false));
        
        getKeymap().setDefaultAction(new DefaultKeyAction());
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
        } finally {
            entireEdit.end();
        }
    }
    
    /** Returns the indenter in use for this file.
     */
    public Indenter getIndenter() {
        return indenter;
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
            String indentString = Parameters.getParameter("indent.string", "\t");
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
    
// FIXME: incorporate JavaDoc comment formatting in 'correctIndentation'.
//        [this code came from InsertNewlineAction.]
//        try {
//            int position = target.getCaretPosition();
//            //target.getIndentationOfLineAtOffset(position));
//            String line = target.getLineTextAtOffset(position);
//            StringBuffer whitespace = getIndentation(line);
//            Log.warn(">>" + line + "<<");
//            if (line.endsWith("{\n")) {
//                //FIXME: it would be nice to insert the closing brace too, don't you think?
//                whitespace.append(Parameters.getParameter("indent.string", "\t"));
//            } else if (line.endsWith("/**\n")) {
//                whitespace.append(" *");
//            } else if ((line.length() == whitespace.length() + 3) && line.endsWith("*/\n")) {
//                whitespace.setLength(whitespace.length() - 1); // Remove " ".
//            }
//            target.replaceSelection("\n" + whitespace);
//        } catch (BadLocationException ex) {
//            ex.printStackTrace();
//        }
//    public boolean isWhitespace(String line, int offset) {
//        if (offset >= line.length()) {
//            return false;
//        }
//        char ch = line.charAt(offset);
//        return ch == ' ' || ch == '\t';
//    }
//    public StringBuffer getIndentation(String line) {
//        StringBuffer result = new StringBuffer();
//        boolean worthContinuing = true;
//        for (int i = 0; worthContinuing && i < line.length(); i++) {
//            char ch = line.charAt(i);
//            if (isWhitespace(line, i)) {
//                result.append(ch);
//            } else {
//                worthContinuing = false;
//                if (ch == '*' && isWhitespace(line, i + 1)) {
//                    result.append(ch);
//                }
//            }
//        }
//        return result;
//    }
    
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
}
