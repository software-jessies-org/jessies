package e.edit;

import java.awt.event.*;
import javax.swing.undo.*;

/**
The ETextArea undo action.
*/
public class UndoAction extends ETextAction {
    public static final String ACTION_NAME = "Undo";

    public UndoAction() {
        super(ACTION_NAME);
    }

    public void actionPerformed(ActionEvent e) {
        ETextArea target = getTextArea();
        if (target == null) {
            return;
        }
        try {
            // Workaround for Java Bug Parade #4688560.
            target.setCaretPosition(target.getCaretPosition());

            UndoManager undoManager = target.getUndoManager();
            if (undoManager.canUndo() == false) {
                return; // Maybe we were called via C-Z rather than from the menu; whatever, there's nothing to undo.
            }
            
            undoManager.undo();
            if (undoManager.canUndo() == false) {
                getFocusedTextWindow().markAsClean();
            }
        } catch (CannotUndoException ex) {
            ex.printStackTrace();
        }
    }
}
