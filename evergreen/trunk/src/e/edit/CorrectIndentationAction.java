package e.edit;

import java.awt.event.*;

/**
 * The ETextArea action that automatically corrects the current line's indentation.
 */
public class CorrectIndentationAction extends ETextAction {
    public static final String ACTION_NAME = "Correct Indentation";

    public CorrectIndentationAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("I", false));
    }
    
    public void actionPerformed(ActionEvent e) {
        getTextArea().fixIndentation();
    }
}
