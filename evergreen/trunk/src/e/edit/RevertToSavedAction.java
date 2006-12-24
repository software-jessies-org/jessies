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
    
    public boolean isEnabled() {
        ETextWindow textWindow = getFocusedTextWindow();
        return (textWindow != null && textWindow.canRevertToSaved());
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        if (window == null) {
            return;
        }
        window.revertToSaved();
    }
}
