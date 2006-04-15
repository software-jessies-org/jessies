package e.edit;

import e.gui.*;
import e.ptextarea.*;
import java.awt.event.*;

/**
 * The ETextArea action that automatically corrects the current line's indentation.
 */
public class CorrectIndentationAction extends ETextAction {
    public static final String ACTION_NAME = "Correct Indentation";

    public CorrectIndentationAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("I", false));
        GnomeStockIcon.useStockIcon(this, "gtk-indent");
    }
    
    public void actionPerformed(ActionEvent e) {
        PTextArea textArea = getFocusedTextArea();
        int position = textArea.getSelectionStart();
        if (position == textArea.getSelectionEnd()) {
            int desiredLineIndex = textArea.getLineOfOffset(position) + 1;
            textArea.getIndenter().fixIndentation();
            desiredLineIndex = Math.min(desiredLineIndex, textArea.getLineCount() - 1);
            int desiredPosition = textArea.getLineStartOffset(desiredLineIndex);
            textArea.select(desiredPosition, desiredPosition);
        } else {
            textArea.getIndenter().fixIndentation();
        }
    }
}
