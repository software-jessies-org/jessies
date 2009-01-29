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
    
    public static ETextArea getFocusedTextArea() {
        Component focusedComponent = getFocusedComponent();
        if (focusedComponent instanceof ETextArea) {
            return (ETextArea) focusedComponent;
        }
        return (ETextArea) SwingUtilities.getAncestorOfClass(ETextArea.class, focusedComponent);
    }
    
    public static ETextWindow getFocusedTextWindow() {
        Component focusedComponent = getFocusedComponent();
        if (focusedComponent instanceof ETextArea == false) {
            return null;
        }
        ETextArea target = (ETextArea) focusedComponent;
        ETextWindow textWindow = (ETextWindow) SwingUtilities.getAncestorOfClass(ETextWindow.class, target);
        return textWindow;
    }
    
    public static String getSelectedText() {
        return (getFocusedTextArea() != null) ? getFocusedTextArea().getSelectedText() : "";
    }
    
    /**
     * Returns the currently-selected text, or the word at the caret, from the text area with the focus.
     * Returns the empty string if no text area has the focus.
     */
    public static String getSearchTerm() {
        ETextArea textArea = ETextAction.getFocusedTextArea();
        if (textArea == null) {
            return "";
        }
        
        // We use the selection, if there is one.
        String selection = textArea.getSelectedText();
        if (selection.length() > 0) {
            return selection.trim();
        }
        
        // Otherwise, we use the word at the caret.
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
