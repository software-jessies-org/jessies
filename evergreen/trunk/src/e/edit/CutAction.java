package e.edit;

import java.awt.event.*;

/**
 * FIXME: PTextArea should export a cut action.
 */
public class CutAction extends ETextAction {
    public static final String ACTION_NAME = "Cut";

    public CutAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("X", false));
    }

    public void actionPerformed(ActionEvent e) {
        getTextArea().cut();
    }
}
