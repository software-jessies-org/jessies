package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class KillErrorsAction extends AbstractAction {
    public KillErrorsAction() {
        super("Clear Errors");
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("K", false));
    }
    
    public void actionPerformed(ActionEvent e) {
        Workspace workspace = Evergreen.getInstance().getCurrentWorkspace();
        if (workspace == null) {
            return;
        }
        // FIXME: if (as is reasonably likely) the focus was in an errors window, we should apply to that specific one.
        workspace.clearTopErrorsWindow();
    }
}
