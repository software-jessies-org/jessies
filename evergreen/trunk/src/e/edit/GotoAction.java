package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.event.*;

/**
The ETextArea action to open a 'goto' dialog.
*/
public class GotoAction extends ETextAction implements MinibufferUser {
    public ETextArea currentTextArea;
    public int initialCaretPosition;
    
    public GotoAction() {
        super("Go to Line...");
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("L", false));
        GnomeStockIcon.configureAction(this);
    }
    
    public void actionPerformed(ActionEvent e) {
        currentTextArea = getFocusedTextArea();
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
    
    /** Returns false because we offer no actions, so the minibuffer should remain active. */
    public boolean interpretSpecialKeystroke(KeyEvent e) {
        return false;
    }
    
    public boolean wasAccepted(String value) {
        valueChangedTo(value);
        return true;
    }
    
    public void wasCanceled() {
        currentTextArea.setCaretPosition(initialCaretPosition);
    }
}
