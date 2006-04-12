package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class RemoveWorkspaceAction extends AbstractAction {
    public static final String ACTION_NAME = "Remove Workspace";
    private int workspaceIndex = -1;
    
    public RemoveWorkspaceAction() {
        super(ACTION_NAME);
    }
    
    public RemoveWorkspaceAction(int workspaceIndex) {
        super(ACTION_NAME);
        this.workspaceIndex = workspaceIndex;
    }
    
    public void actionPerformed(ActionEvent e) {
        Edit edit = Edit.getInstance();
        if (workspaceIndex == -1) {
            edit.removeCurrentWorkspace();
        } else {
            edit.removeWorkspace(workspaceIndex);
        }
    }
}
