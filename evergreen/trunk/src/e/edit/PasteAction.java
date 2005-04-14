package e.edit;

import java.awt.event.*;

/**
 * FIXME: PTextArea should export a paste action.
 */
public class PasteAction extends ETextAction {
    public static final String ACTION_NAME = "Paste";

    public PasteAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("V", false));
    }

    public void actionPerformed(ActionEvent e) {
        getTextArea().paste();
    }
}
