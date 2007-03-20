package e.edit;

import e.gui.*;
import java.awt.event.*;

public class SaveAction extends ETextAction {
    public SaveAction() {
        super("Save");
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("S", false));
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
