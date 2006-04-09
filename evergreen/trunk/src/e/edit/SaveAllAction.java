package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class SaveAllAction extends AbstractAction {
    private static final String ACTION_NAME = "Save All";
    
    public SaveAllAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        saveAll(true);
    }

    public static void saveAll(boolean interactive) {
        for (Workspace workspace : Edit.getInstance().getWorkspaces()) {
            if (workspace.saveAll() == false) {
                if (interactive) {
                    Edit.getInstance().showAlert("Couldn't save all", "Unable to save everything on workspace '" + workspace.getTitle() + "'.");
                }
                return;
            }
        }
    }
}
