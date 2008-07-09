package e.edit;

import e.ptextarea.*;
import java.awt.event.*;

/**
 * Joins the current line with the next.
 */
public class JoinLinesAction extends ETextAction {
    public JoinLinesAction() {
        super("Join Lines");
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("J", true));
    }
    
    public void actionPerformed(ActionEvent e) {
        PTextArea textArea = getFocusedTextArea();
        PTextBuffer buffer = textArea.getTextBuffer();
        // FIXME: would it be useful to join all the lines in the selection, if there is one?
        int startIndex = textArea.getSelectionStart();
        while (startIndex < buffer.length() && buffer.charAt(startIndex) != '\n') {
            ++startIndex;
        }
        if (startIndex >= buffer.length()) {
            return;
        }
        int endIndex = startIndex + 1;
        while (endIndex < buffer.length() && Character.isWhitespace(buffer.charAt(endIndex))) {
            ++endIndex;
        }
        // FIXME: delete, or replace with a single ' '? what if the current line ends with whitespace?
        textArea.delete(startIndex, endIndex - startIndex);
    }
}
