package e.edit;

import java.awt.event.*;
import javax.swing.*;

/**
The ETextArea close window action.
*/
public class CloseWindowAction extends ETextAction {
    public static final String ACTION_NAME = "Close";

    public CloseWindowAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, EditMenuBar.makeKeyStroke("W", false));
    }

    public void actionPerformed(ActionEvent e) {
        EWindow window = (EWindow) SwingUtilities.getAncestorOfClass(EWindow.class, getFocusedComponent());
        if (window != null) {
            window.closeWindow();
        }
    }
}
