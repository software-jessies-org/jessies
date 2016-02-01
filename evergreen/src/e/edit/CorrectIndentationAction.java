package e.edit;

import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.event.*;

/**
 * Automatically corrects the current line's indentation.
 */
public class CorrectIndentationAction extends ETextAction {
    public CorrectIndentationAction() {
        super("Correct _Indentation", GuiUtilities.makeKeyStroke("I", false));
        GnomeStockIcon.useStockIcon(this, "gtk-indent");
    }
    
    public void actionPerformed(ActionEvent e) {
        PTextArea textArea = getFocusedTextArea();
        if (textArea == null) {
            return;
        }
        
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
