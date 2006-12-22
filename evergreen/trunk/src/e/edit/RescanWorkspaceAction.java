package e.edit;

import e.gui.*;
import java.awt.event.*;
import javax.swing.*;

public class RescanWorkspaceAction extends AbstractAction {
    public static final String ACTION_NAME = "Rescan Files";
    
    private Workspace boundWorkspace;
    
    // Rescan the given workspace.
    public RescanWorkspaceAction(Workspace workspace) {
        super(ACTION_NAME);
        this.boundWorkspace = workspace;
        GnomeStockIcon.useStockIcon(this, "gtk-refresh");
    }
    
    // Rescan the current workspace at the time the action is performed.
    public RescanWorkspaceAction() {
        this(null);
    }
    
    public void actionPerformed(ActionEvent e) {
        Workspace workspace = (boundWorkspace != null) ? boundWorkspace : Evergreen.getInstance().getCurrentWorkspace();
        workspace.getFileList().updateFileList();
    }
    
    public static JButton makeRescanButton(Workspace workspace) {
        AbstractAction action = new RescanWorkspaceAction(workspace);
        // Use a shorter name for buttons than menus...
        action.putValue(NAME, "Rescan");
        JButton result = new JButton(action);
        // Use a larger icon for buttons than menus...
        GnomeStockIcon.useStockIcon(result, "gtk-refresh");
        return result;
    }
}
