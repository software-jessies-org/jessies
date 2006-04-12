package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class RescanWorkspaceAction extends AbstractAction {
    public static final String ACTION_NAME = "Rescan Files";
    private int workspaceIndex = -1;
    
    public RescanWorkspaceAction() {
        super(ACTION_NAME);
    }
    
    public RescanWorkspaceAction(int workspaceIndex) {
        super(ACTION_NAME);
        this.workspaceIndex = workspaceIndex;
    }
    
    public void actionPerformed(ActionEvent e) {
        System.err.println("Event source: " + e.getSource());
        Edit edit = Edit.getInstance();
        if (workspaceIndex == -1) {
            edit.getCurrentWorkspace().updateFileList(null);
        } else {
            edit.getWorkspaces()[workspaceIndex].updateFileList(null);
        }
    }
}
