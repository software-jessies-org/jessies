package e.edit;

import e.util.*;
import java.awt.event.*;
import javax.swing.*;

public class CloseWorkspaceAction extends AbstractAction {
    private Workspace boundWorkspace;
    
    // Close the given workspace.
    public CloseWorkspaceAction(Workspace workspace) {
        GuiUtilities.configureAction(this, "_Close Workspace", null);
        this.boundWorkspace = workspace;
    }
    
    // Close the current workspace at the time the action is performed.
    public CloseWorkspaceAction() {
        this(null);
    }
    
    @Override public boolean isEnabled() {
        return !Evergreen.getInstance().getWorkspaces().isEmpty();
    }
    
    public void actionPerformed(ActionEvent e) {
        Workspace workspace = (boundWorkspace != null) ? boundWorkspace : Evergreen.getInstance().getCurrentWorkspace();
        Evergreen.getInstance().closeWorkspace(workspace);
    }
}
