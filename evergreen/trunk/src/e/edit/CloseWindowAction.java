package e.edit;

import e.gui.*;
import java.awt.event.*;
import javax.swing.*;

/**
The ETextArea close window action.
*/
public class CloseWindowAction extends ETextAction {
    public static final String ACTION_NAME = "Close";

    public CloseWindowAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("W", false));
        GnomeStockIcon.useStockIcon(this, "gtk-close");
    }

    public void actionPerformed(ActionEvent e) {
        EWindow window = (EWindow) SwingUtilities.getAncestorOfClass(EWindow.class, getFocusedComponent());
        if (window != null) {
            window.closeWindow();
        }
    }
}
