package e.edit;

import java.awt.event.*;

/**
The ETextArea action to cut to the clipboard.
*/
public class CutAction extends ETextAction {
    public static final String ACTION_NAME = "Cut";

    public CutAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, EditMenuBar.makeKeyStroke("X", false));
    }

    public void actionPerformed(ActionEvent e) {
        ETextArea target = (ETextArea) getFocusedComponent();
        target.cut();
    }
}
