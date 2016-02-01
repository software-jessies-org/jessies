package e.edit;

import e.ptextarea.*;
import e.util.*;
import java.awt.event.*;

/**
 * Joins the current line with the next.
 */
public class JoinLinesAction extends ETextAction {
    public JoinLinesAction() {
        super("_Join Lines", GuiUtilities.makeKeyStroke("J", true));
    }
    
    public void actionPerformed(ActionEvent e) {
        PTextArea textArea = getFocusedTextArea();
        if (textArea == null) {
            return;
        }
        
        PTextBuffer buffer = textArea.getTextBuffer();
        // FIXME: would it be useful to join all the lines in the selection, if there is one?
        int startIndex = textArea.getSelectionStart();
        while (startIndex < buffer.length() && buffer.charAt(startIndex) != '\n') {
            ++startIndex;
        }
        if (startIndex >= buffer.length()) {
            return;
        }
        // We remove the newline itself...
        int endIndex = startIndex + 1;
        // ...and also any run of whitespace that follows (typically the next line's indentation, which is no longer needed).
        while (endIndex < buffer.length() && Character.isWhitespace(buffer.charAt(endIndex))) {
            ++endIndex;
        }
        // We want vi(1)-like behavior where if the current line doesn't already end in a space, we append one.
        String join = (startIndex > 0 && buffer.charAt(startIndex - 1) != ' ') ? " " : "";
        textArea.replaceRange(join, startIndex, endIndex);
    }
}
