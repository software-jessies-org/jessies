package e.edit;

import java.awt.event.*;
import javax.swing.*;
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
            String line = target.getLineTextAtOffset(position);
            if (target.getIndenter().isElectric('}') && line.endsWith("{") && hasUnbalancedBraces(target.getText())) {
                insertMatchingBrace(target);
            } else if (line.endsWith("/*") || line.endsWith("/**")) {
                insertMatchingCloseComment(target);
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
            ETextWindow textWindow = (ETextWindow) SwingUtilities.getAncestorOfClass(ETextWindow.class, target);
            boolean mightNeedSemicolon = textWindow != null && textWindow.isCPlusPlus();
            
            final int position = target.getCaretPosition();
            String line = target.getLineTextAtOffset(position);
            String whitespace = target.getIndentationOfLineAtOffset(position);
            String prefix = "\n" + whitespace + target.getIndentationString();
            String suffix = "\n" + whitespace + "}";
            if (mightNeedSemicolon && line.matches(".*\\b(class|enum|struct|union)\\b.*")) {
                // These C constructs need a semicolon after the closing brace.
                suffix += ";";
            }
            target.getDocument().insertString(position, prefix + suffix, null);
            target.setCaretPosition(position + prefix.length());
        } catch (BadLocationException ex) {
            Log.warn("Problem inserting brace pair.", ex);
        }
    }
    
    public void insertMatchingCloseComment(ETextArea target) {
        try {
            final int position = target.getCaretPosition();
            String line = target.getLineTextAtOffset(position);
            String whitespace = target.getIndentationOfLineAtOffset(position);
            String prefix = "\n" + whitespace + " * ";
            String suffix = "\n" + whitespace + " */";
            target.getDocument().insertString(position, prefix + suffix, null);
            target.setCaretPosition(position + prefix.length());
        } catch (BadLocationException ex) {
            Log.warn("Problem inserting close comment.", ex);
        }
    }
    
    public boolean hasUnbalancedBraces(String text) {
        int braceNesting = 0;
        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (ch == '{') {
                ++braceNesting;
            } else if (ch == '}') {
                --braceNesting;
            }
        }
        return (braceNesting != 0);
    }
}
