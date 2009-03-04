package e.edit;

import e.util.*;
import java.awt.event.*;
import javax.swing.*;

public class MergeWorkspaceAction extends AbstractAction {
    private Workspace boundWorkspace;
    
    // Merge the given workspace.
    public MergeWorkspaceAction(Workspace workspace) {
        GuiUtilities.configureAction(this, "_Merge Workspace", null);
        this.boundWorkspace = workspace;
    }
    
    // Merge the current workspace at the time the action is performed.
    public MergeWorkspaceAction() {
        this(null);
    }
    
    public void actionPerformed(ActionEvent e) {
        Workspace workspace = (boundWorkspace != null) ? boundWorkspace : Evergreen.getInstance().getCurrentWorkspace();
        Evergreen.getInstance().mergeWorkspace(workspace);
    }
}
