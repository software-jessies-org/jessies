package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.event.*;

public class SaveAction extends ETextAction {
    public SaveAction() {
        super("_Save", GuiUtilities.makeKeyStroke("S", false));
        GnomeStockIcon.configureAction(this);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        if (window == null) {
            return;
        }
        window.save();
    }
}
