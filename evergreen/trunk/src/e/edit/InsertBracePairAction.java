package e.edit;

import java.awt.event.*;
import javax.swing.text.*;

import e.util.*;

/**
The ETextArea action that inserts a pair of braces.
*/
public class InsertBracePairAction extends TextAction {
    public static final String ACTION_NAME = "insert-brace-pair";

    public InsertBracePairAction() {
        super(ACTION_NAME);
    }

    public void actionPerformed(ActionEvent e) {
        ETextArea target = (ETextArea) getFocusedComponent();
        try {
            Document document = target.getDocument();
            int position = target.getCaretPosition();
            String whitespace = target.getIndentationOfLineAtOffset(position);
            String prefix = "{\n" + whitespace + Parameters.getParameter("indent.string");
            String suffix = "\n" + whitespace + "}";
            document.insertString(position, prefix + suffix, null);
            target.setCaretPosition(position + prefix.length());
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }
}
