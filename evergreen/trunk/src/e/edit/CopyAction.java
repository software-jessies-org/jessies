package e.edit;

import java.awt.event.*;

/**
 * FIXME: PTextArea should export a copy action.
 */
public class CopyAction extends ETextAction {
    public static final String ACTION_NAME = "Copy";

    public CopyAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("C", false));
    }

    public void actionPerformed(ActionEvent e) {
        getTextArea().copy();
    }
}
