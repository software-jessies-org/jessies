package e.edit;

import e.util.*;
import e.ptextarea.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public abstract class ETextAction extends AbstractAction {
    public ETextAction(String name, KeyStroke keystroke) {
        GuiUtilities.configureAction(this, name, keystroke);
    }
    
    // If you don't require a focused text window, you probably shouldn't be an ETextAction.
    // Feel free to override if you have more specialist needs (but don't forget this check).
    @Override public boolean isEnabled() {
        return (getFocusedTextWindow() != null);
    }
    
    public abstract void actionPerformed(ActionEvent e);

    public static Component getFocusedComponent() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
    }
    
    public static PTextArea getFocusedTextArea() {
        Component focusedComponent = getFocusedComponent();
        if (focusedComponent instanceof PTextArea) {
            return (PTextArea) focusedComponent;
        }
        return (PTextArea) SwingUtilities.getAncestorOfClass(PTextArea.class, focusedComponent);
    }
    
    public static ETextWindow getFocusedTextWindow() {
        Component focusedComponent = getFocusedComponent();
        if (focusedComponent instanceof PTextArea == false) {
            return null;
        }
        PTextArea target = (PTextArea) focusedComponent;
        ETextWindow textWindow = (ETextWindow) SwingUtilities.getAncestorOfClass(ETextWindow.class, target);
        return textWindow;
    }
    
    public static String getSelectedText() {
        return (getFocusedTextArea() != null) ? getFocusedTextArea().getSelectedText() : "";
    }
    
    /**
     * Returns a regular expression that would match the selection (less trailing newlines), or the empty string.
     * The empty string is returned if no text area has focus, the focused text area has no selection, or the selection contains newlines.
     */
    public static String getSelectedRegularExpression() {
        // Get the selection, stripping trailing newlines.
        // ETextAction.getSelectedText returns the empty string if no text area has focus.
        final String selection = ETextAction.getSelectedText().replaceAll("\n$", "");
        
        // Only use the selection as a regular expression if there are no embedded newlines.
        if (selection.isEmpty() || selection.contains("\n")) {
            return "";
        }
        return StringUtilities.regularExpressionFromLiteral(selection);
    }
    
    /**
     * Returns the currently-selected text, or the word at the caret, from the text area with the focus.
     * Returns the empty string if no text area has the focus.
     */
    public static String getSearchTerm() {
        PTextArea textArea = ETextAction.getFocusedTextArea();
        if (textArea == null) {
            return "";
        }
        
        // We use the selection, if there is one.
        String selection = textArea.getSelectedText();
        if (selection.length() > 0) {
            return selection.trim();
        }
        
        // Otherwise, we use the word at the caret.
        return getWordAtCaret(textArea);
    }
    
    public static String getWordAtCaret(PTextArea textArea) {
        CharSequence chars = textArea.getTextBuffer();
        String stopChars = chooseStopChars();
        int caretPosition = textArea.getSelectionStart();
        int start = PWordUtilities.getWordStart(chars, caretPosition, stopChars);
        int end = PWordUtilities.getWordEnd(chars, caretPosition, stopChars);
        return chars.subSequence(start, end).toString();
    }
    
    private static String chooseStopChars() {
        FileType fileType = ETextAction.getFocusedTextWindow().getFileType();
        String stopChars = PWordUtilities.DEFAULT_STOP_CHARS;
        if (fileType == FileType.C_PLUS_PLUS) {
            // "::" is useful in C++, so remove it from the stop list.
            // An alternative would be to stop insisting on the "std::" prefix for STL lookups, but that would introduce ambiguity: std::string versus string(3), for example.
            stopChars = stopChars.replace(":", "");
        } else if (fileType == FileType.PYTHON) {
            // "." is useful in Python, because pydoc(1) wants fully-qualified names.
            stopChars = stopChars.replace(".", "");
        }
        return stopChars;
    }
}
