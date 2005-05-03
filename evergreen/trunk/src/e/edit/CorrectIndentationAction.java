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
        // Desired semantics:
        // If there's a selection, fix the indentation of all the lines within the selection (and leave it selected?).
        // (If the selection ends at the start of a line, that line shouldn't be touched.)
        // If there's no selection, fix the indentation of the current line and move the caret to the next line.
        // That hack should be in this function.
        getTextArea().getIndenter().fixIndentation(true);
    }
}
