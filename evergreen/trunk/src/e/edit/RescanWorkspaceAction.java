package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class RescanWorkspaceAction extends AbstractAction {
    public static final String ACTION_NAME = "Rescan Files";
    
    private Workspace boundWorkspace;
    
    // Rescan the given workspace.
    public RescanWorkspaceAction(Workspace workspace) {
        super(ACTION_NAME);
        this.boundWorkspace = workspace;
    }
    
    // Rescan the current workspace at the time the action is performed.
    public RescanWorkspaceAction() {
        this(null);
    }
    
    public void actionPerformed(ActionEvent e) {
        Workspace workspace = (boundWorkspace != null) ? boundWorkspace : Edit.getInstance().getCurrentWorkspace();
        workspace.updateFileList(null);
    }
}
