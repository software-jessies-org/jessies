package e.edit;

import e.gui.*;
import java.awt.event.*;
import javax.swing.*;

/**
The ETextArea close window action.
*/
public class CloseWindowAction extends ETextAction {
    public CloseWindowAction() {
        super("Close");
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("W", false));
        GnomeStockIcon.configureAction(this);
    }

    public void actionPerformed(ActionEvent e) {
        EWindow window = (EWindow) SwingUtilities.getAncestorOfClass(EWindow.class, getFocusedComponent());
        if (window != null) {
            window.closeWindow();
        }
    }
}
