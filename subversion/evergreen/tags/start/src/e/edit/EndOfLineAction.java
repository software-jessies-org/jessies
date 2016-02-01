package e.edit;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

/**
The ETextArea go to/select to end of line action.
*/
public class EndOfLineAction extends TextAction {
    public static final String ACTION_NAME = "EndOfLine";
    public static final String SHIFT_ACTION_NAME = "SelectToEndOfLine";
    
    private boolean select;
    
    public EndOfLineAction(boolean select) {
        super(select ? SHIFT_ACTION_NAME : ACTION_NAME);
        this.select = select;
    }
    
    public ETextComponent getTextArea() {
        Component component = getFocusedComponent();
        return (ETextComponent) (SwingUtilities.getAncestorOfClass(ETextComponent.class, component));
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextArea target = (ETextArea) getFocusedComponent();
        if (target != null) {
            try {
                int offset = target.getCaretPosition();
                int lineEndOffset = target.getLineEndOffset(target.getLineOfOffset(offset));
                if (lineEndOffset != target.getDocument().getLength()) {
                    lineEndOffset--; // Back up past the newline.
                }
                if (select) {
                    target.moveCaretPosition(lineEndOffset);
                } else {
                    target.setCaretPosition(lineEndOffset);
                }
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }
    }
}
