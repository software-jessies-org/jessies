package e.edit;

import java.awt.event.*;
import javax.swing.text.*;

/**
The ETextArea action to open a 'goto' dialog.
*/
public class GotoAction extends ETextAction implements MinibufferUser {
    public static final String ACTION_NAME = "Go to Line...";
    
    public ETextWindow currentTextWindow;
    
    public GotoAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        currentTextWindow = getFocusedTextWindow();
        if (currentTextWindow == null) {
            return;
        }
        
        Edit.showMinibuffer(this);
    }
    
    //
    // MinibufferUser interface.
    //
    
    public String getInitialValue() {
        String result = "";
        try {
            ETextArea textArea = currentTextWindow.getText();
            int lineNumber = 1 + textArea.getLineOfOffset(textArea.getCaretPosition());
            result = Integer.toString(lineNumber);
        } catch (BadLocationException ex) {
            // Ignore.
            ex = ex;
        }
        return result;
    }
    
    public String getPrompt() {
        return "Go To Line";
    }
    
    /** Checks whether the line number is a number, and is less than the file's line count. */
    public boolean isValid(String value) {
        try {
            int line = Integer.parseInt(value);
            return line < currentTextWindow.getText().getLineCount();
        } catch (NumberFormatException ex) {
            return false;
        }
    }
    
    public void valueChangedTo(String value) {
    }
    
    /** Returns false because we offer no actions, so the minibuffer should remain active. */
    public boolean interpretSpecialKeystroke(KeyEvent e) {
        return false;
    }
    
    public boolean wasAccepted(String value) {
        try {
            int line = Integer.parseInt(value);
            if (line < currentTextWindow.getText().getLineCount()) {
                currentTextWindow.goToLine(line);
                return true;
            }
            Edit.showAlert("Go To", "There is no line " + line + ".");
        } catch (NumberFormatException ex) {
            Edit.showAlert("Go To", "The text '" + value + "' isn't a line number.");
        }
        return false;
    }
    
    public void wasCanceled() {
    }
}
