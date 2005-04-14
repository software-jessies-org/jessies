package e.edit;

import java.awt.event.*;

/**
 * FIXME: PTextArea should *export* a redo action.
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
        target.getPTextBuffer().getUndoBuffer().redo();
    }
}
