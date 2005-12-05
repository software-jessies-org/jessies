package e.edit;

import e.gui.*;
import java.awt.event.*;

/**
The ETextArea revert-to-saved action.
*/
public class RevertToSavedAction extends ETextAction {
    public static final String ACTION_NAME = "Revert to Saved";
    
    public RevertToSavedAction() {
        super(ACTION_NAME);
        GnomeStockIcon.useStockIcon(this, "gtk-revert-to-saved");
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        if (window == null) {
            return;
        }
        window.revertToSaved();
    }
}
