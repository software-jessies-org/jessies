package e.edit;

import java.awt.event.*;
import javax.swing.text.*;

/**
The ETextArea action that automatically corrects the current line's indentation.
*/
public class CorrectIndentationAction extends TextAction {
    public static final String ACTION_NAME = "correct-indentation";

    public CorrectIndentationAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextArea target = (ETextArea) getFocusedComponent();
        target.correctIndentation();
    }
}
