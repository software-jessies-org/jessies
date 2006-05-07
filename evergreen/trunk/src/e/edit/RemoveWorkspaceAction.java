package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class RemoveWorkspaceAction extends AbstractAction {
    public static final String ACTION_NAME = "Remove Workspace";
    
    private Workspace boundWorkspace;
    
    // Remove the given workspace.
    public RemoveWorkspaceAction(Workspace workspace) {
        super(ACTION_NAME);
        this.boundWorkspace = workspace;
    }
    
    // Remove the current workspace at the time the action is performed.
    public RemoveWorkspaceAction() {
        this(null);
    }
    
    public void actionPerformed(ActionEvent e) {
        Workspace workspace = (boundWorkspace != null) ? boundWorkspace : Evergreen.getInstance().getCurrentWorkspace();
        Evergreen.getInstance().removeWorkspace(workspace);
    }
}
