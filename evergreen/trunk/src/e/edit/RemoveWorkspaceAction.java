package e.edit;

import java.awt.event.*;
import javax.swing.*;

/**
 * FIXME: Should open a dialog where the user can specify which workspace to remove?
 */
public class RemoveWorkspaceAction extends AbstractAction {
    public static final String ACTION_NAME = "Remove Current Workspace";
    
    public RemoveWorkspaceAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        Edit.removeCurrentWorkspace();
    }
}
