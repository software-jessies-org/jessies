package e.edit;

import java.awt.event.*;

/**
The ETextArea action to copy to the clipboard.
*/
public class CopyAction extends ETextAction {
    public static final String ACTION_NAME = "Copy";

    public CopyAction() {
        super(ACTION_NAME);
    }

    public void actionPerformed(ActionEvent e) {
        ETextArea target = (ETextArea) getFocusedComponent();
        target.copy();
    }
}
