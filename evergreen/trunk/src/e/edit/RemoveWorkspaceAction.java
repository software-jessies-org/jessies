package e.edit;

import e.util.*;
import java.awt.event.*;
import javax.swing.*;

public class RemoveWorkspaceAction extends AbstractAction {
    private Workspace boundWorkspace;
    
    // Remove the given workspace.
    public RemoveWorkspaceAction(Workspace workspace) {
        GuiUtilities.configureAction(this, "Remo_ve Workspace", null);
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
