package e.edit;

import java.awt.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

import e.util.*;

/**
The ETextArea action that inserts a newline and performs auto-indentation.
*/
public class InsertNewlineAction extends TextAction {
    public static final String ACTION_NAME = "insert-newline-and-auto-indent";

    public InsertNewlineAction() {
        super(ACTION_NAME);
    }

    public void actionPerformed(ActionEvent e) {
        ETextArea target = (ETextArea) getFocusedComponent();
        CompoundEdit entireEdit = new CompoundEdit();
        target.getUndoManager().addEdit(entireEdit);
        try {
            Document document = target.getDocument();
            final int position = target.getCaretPosition();
            
            // Should we try to insert matching brace pairs?
            String previousCharacter = (position == 0) ? "" : document.getText(position - 1, 1);
            if (target.getIndenter().isElectric('}') && previousCharacter.equals("{")) {
                System.err.println("insert brace pair");
                insertMatchingBrace(target);
            } else {
                target.replaceSelection("\n");
                target.autoIndent();
            }
        } catch (BadLocationException ex) {
            Log.warn("Problem inserting newline.", ex);
        } finally {
            entireEdit.end();
        }
    }
    
    public void insertMatchingBrace(ETextArea target) {
        try {
            final int position = target.getCaretPosition();
            String whitespace = target.getIndentationOfLineAtOffset(position);
            String prefix = "\n" + whitespace + target.getIndentationString();
            String suffix = "\n" + whitespace + "}";
            target.getDocument().insertString(position, prefix + suffix, null);
            target.setCaretPosition(position + prefix.length());
        } catch (BadLocationException ex) {
            Log.warn("Problem inserting brace pair.", ex);
        }
    }
}
