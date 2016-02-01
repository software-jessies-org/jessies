package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class ClearErrorsAction extends AbstractAction {
    public ClearErrorsAction() {
        super("Clear Errors");
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("K", false));
    }
    
    @Override public boolean isEnabled() {
        return !Evergreen.getInstance().getWorkspaces().isEmpty();
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
