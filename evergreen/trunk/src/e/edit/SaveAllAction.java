package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class SaveAllAction extends AbstractAction {
    public static final String ACTION_NAME = "Save All";
    
    public SaveAllAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        Workspace[] workspaces = Edit.getWorkspaces();
        for (int i = 0; i < workspaces.length; ++i) {
            Workspace workspace = workspaces[i];
            if (workspace.saveAll() == false) {
                Edit.showAlert("Save All", "Unable to save everything on workspace '" + workspace.getTitle() + "'.");
                return;
            }
        }
    }
}
