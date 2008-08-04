package e.edit;

import e.gui.*;
import java.awt.event.*;

/**
The ETextArea revert-to-saved action.
*/
public class RevertToSavedAction extends ETextAction {
    public RevertToSavedAction() {
        super("Revert to Saved", null);
        GnomeStockIcon.configureAction(this);
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
