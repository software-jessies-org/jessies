package e.edit;

import java.awt.event.*;
import javax.swing.undo.*;

/**
The ETextArea redo action.
*/
public class RedoAction extends ETextAction {
    public static final String ACTION_NAME = "Redo";

    public RedoAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("Z", true));
    }

    public void actionPerformed(ActionEvent e) {
        ETextArea target = getTextArea();
        if (target == null) {
            return;
        }
        try {
            target.setCaretPosition(target.getCaretPosition()); // Workaround for Java Bug Parade #4688560.
            target.getUndoManager().redo();
        } catch (CannotUndoException ex) {
            ex.printStackTrace();
        }
    }
}
