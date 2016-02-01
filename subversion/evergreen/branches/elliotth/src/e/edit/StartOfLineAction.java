package e.edit;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

/**
The ETextArea go to/select to start of line action.
*/
public class StartOfLineAction extends TextAction {
    public static final String ACTION_NAME = "StartOfLine";
    public static final String SHIFT_ACTION_NAME = "SelectToStartOfLine";
    
    private boolean select;
    
    public StartOfLineAction(boolean select) {
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
                int lineStartOffset = target.getLineStartOffset(target.getLineOfOffset(offset));
                if (select) {
                    target.moveCaretPosition(lineStartOffset);
                } else {
                    target.setCaretPosition(lineStartOffset);
                }
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }
    }
}
