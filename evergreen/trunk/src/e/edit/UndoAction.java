package e.edit;

import java.awt.event.*;

/**
 * FIXME: PTextArea should *export* an undo action.
 */
public class UndoAction extends ETextAction {
    public static final String ACTION_NAME = "Undo";

    public UndoAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("Z", false));
    }

    public void actionPerformed(ActionEvent e) {
        ETextArea target = getTextArea();
        if (target == null) {
            return;
        }
        target.getPTextBuffer().getUndoBuffer().undo();
        // FIXME
        /*
        if (undoManager.canUndo() == false) {
            getFocusedTextWindow().markAsClean();
        }
        */
    }
}
