package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
The ETextArea close window action.
*/
public class CloseWindowAction extends ETextAction {
    public CloseWindowAction() {
        super("_Close", GuiUtilities.makeKeyStroke("W", false));
        GnomeStockIcon.configureAction(this);
    }

    public void actionPerformed(ActionEvent e) {
        EWindow window = (EWindow) SwingUtilities.getAncestorOfClass(EWindow.class, getFocusedComponent());
        if (window != null) {
            window.closeWindow();
        }
    }
}
