package e.edit;

import e.util.*;
import java.awt.event.*;
import javax.swing.*;

public class SaveAllAction extends AbstractAction {
    public SaveAllAction() {
        GuiUtilities.configureAction(this, "Save A_ll", null);
    }
    
    @Override public boolean isEnabled() {
        for (Workspace workspace : Evergreen.getInstance().getWorkspaces()) {
            if (!workspace.getDirtyTextWindows().isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    public void actionPerformed(ActionEvent e) {
        saveAll(true);
    }

    public static void saveAll(boolean interactive) {
        for (Workspace workspace : Evergreen.getInstance().getWorkspaces()) {
            if (workspace.saveAll() == false) {
                if (interactive) {
                    Evergreen.getInstance().showAlert("Couldn't save all", "Unable to save everything on workspace \"" + workspace.getWorkspaceName() + "\".");
                }
                return;
            }
        }
    }
}
