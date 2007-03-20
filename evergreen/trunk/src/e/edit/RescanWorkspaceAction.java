package e.edit;

import e.gui.*;
import java.awt.event.*;
import javax.swing.*;

public class RescanWorkspaceAction extends AbstractAction {
    private Workspace boundWorkspace;
    
    // Rescan the given workspace.
    public RescanWorkspaceAction(Workspace workspace) {
        this("Rescan Files", workspace);
        GnomeStockIcon.useStockIcon(this, "gtk-refresh");
    }
    
    // Rescan the current workspace at the time the action is performed.
    public RescanWorkspaceAction() {
        this(null);
    }
    
    private RescanWorkspaceAction(String name, Workspace workspace) {
        super(name);
        this.boundWorkspace = workspace;
    }
    
    public void actionPerformed(ActionEvent e) {
        Workspace workspace = (boundWorkspace != null) ? boundWorkspace : Evergreen.getInstance().getCurrentWorkspace();
        workspace.getFileList().updateFileList();
    }
    
    public static JButton makeRescanButton(Workspace workspace) {
        // Use a shorter name for buttons than menus...
        return new JButton(new RescanWorkspaceAction("Rescan", workspace));
    }
}
