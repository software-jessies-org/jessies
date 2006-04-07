package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class RemoveWorkspaceAction extends AbstractAction {
    public static final String ACTION_NAME = "Remove Workspace";
    
    public RemoveWorkspaceAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        Edit.getInstance().removeCurrentWorkspace();
    }
}
