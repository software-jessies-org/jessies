package e.edit;

import java.awt.event.*;

/**
The ETextArea action to paste from the clipboard.
*/
public class PasteAction extends ETextAction {
    public static final String ACTION_NAME = "Paste";

    public PasteAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, EditMenuBar.makeKeyStroke("V", false));
    }

    public void actionPerformed(ActionEvent e) {
        ETextArea target = (ETextArea) getFocusedComponent();
        target.paste();
    }
}
