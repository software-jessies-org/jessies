package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.event.*;

/**
 * Lets the user jump straight to the given line number in the current text.
 */
public class GoToLineAction extends ETextAction implements MinibufferUser {
    private ETextArea currentTextArea;
    private int initialCaretPosition;
    
    public GoToLineAction() {
        super("Go to Line...");
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("L", false));
        GnomeStockIcon.configureAction(this);
    }
    
    public void actionPerformed(ActionEvent e) {
        // See FindAction.actionPerformed.
        ETextWindow newCurrentTextWindow = getFocusedTextWindow();
        if (newCurrentTextWindow == null) {
            return;
        }
        
        currentTextArea = newCurrentTextWindow.getTextArea();
        // FIXME - selection
        initialCaretPosition = currentTextArea.getSelectionStart();
        Evergreen.getInstance().showMinibuffer(this);
    }
    
    //
    // MinibufferUser interface.
    //
    
    public StringHistory getHistory() {
        return null;
    }
    
    public String getInitialValue() {
        // FIXME - selection
        int lineNumber = 1 + currentTextArea.getLineOfOffset(currentTextArea.getSelectionStart());
        return Integer.toString(lineNumber);
    }
    
    public String getPrompt() {
        return "Go To Line";
    }
    
    /** Checks whether the line number is a number, and is less than the file's line count. */
    public boolean isValid(String value) {
        try {
            int line = Integer.parseInt(value);
            return (line > 0 && line <= currentTextArea.getLineCount());
        } catch (NumberFormatException ex) {
            return false;
        }
    }
    
    public void valueChangedTo(String value) {
        if (isValid(value)) {
            currentTextArea.goToLine(Integer.parseInt(value));
        }
    }
    
    public boolean wasAccepted(String value) {
        valueChangedTo(value);
        return true;
    }
    
    public void wasCanceled() {
        currentTextArea.setCaretPosition(initialCaretPosition);
    }
}
