package e.edit;

import java.awt.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

import e.util.*;

public class DefaultKeyAction extends ETextAction {
    public DefaultKeyAction() {
        super("default-key-action-with-electric-key-support");
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextArea target = getTextArea();
        if (target == null || e == null) {
            return;
        }
        
        if ((! target.isEditable()) || (! target.isEnabled())) {
            javax.swing.UIManager.getLookAndFeel().provideErrorFeedback(target);
            return;
        }
        
        String content = e.getActionCommand();
        int mod = e.getModifiers();
        if (content == null || content.length() < 1 || ((mod & ActionEvent.ALT_MASK) != (mod & ActionEvent.CTRL_MASK))) {
            return;
        }
        
        char c = content.charAt(0);
        if ((c < 0x20) || (c == 0x7F)) {
            return;
        }
        
        if (target.getIndenter().isElectric(c)) {
            CompoundEdit entireEdit = new CompoundEdit();
            target.getUndoManager().addEdit(entireEdit);
            try {
                target.replaceSelection(content);
                target.correctIndentation(false);
            } finally {
                entireEdit.end();
            }
        } else if (c == '{' && target.getIndenter().isElectric('}')) {
            insertBracePair(target);
        } else if (content != null) {
            target.replaceSelection(content);
        } else {
            javax.swing.UIManager.getLookAndFeel().provideErrorFeedback(target);
        }
    }
    
    public void insertBracePair(ETextArea target) {
        CompoundEdit entireEdit = null;
        try {
            Document document = target.getDocument();
            int position = target.getCaretPosition();
            
            // An obvious special case we're not interested in.
            // String literals are harder to spot, though.
            if (position > 0 && document.getText(position - 1, 1).equals("'")) {
                target.replaceSelection("{");
                return;
            }
            
            entireEdit = new CompoundEdit();
            target.getUndoManager().addEdit(entireEdit);
            String whitespace = target.getIndentationOfLineAtOffset(position);
            String prefix = "{\n" + whitespace + target.getIndentationString();
            String suffix = "\n" + whitespace + "}";
            document.insertString(position, prefix + suffix, null);
            target.setCaretPosition(position + prefix.length());
        } catch (BadLocationException ex) {
            Log.warn("Problem inserting brace pair.", ex);
        } finally {
            if (entireEdit != null) {
                entireEdit.end();
            }
        }
    }
}
