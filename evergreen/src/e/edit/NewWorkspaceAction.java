package e.edit;

import e.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Opens a dialog where the user can specify the details of a new workspace, which will then be created.
 */
public class NewWorkspaceAction extends AbstractAction {
    public NewWorkspaceAction() {
        GuiUtilities.configureAction(this, "New _Workspace...", null);
    }
    
    public void actionPerformed(ActionEvent e) {
        WorkspaceProperties properties = new WorkspaceProperties();
        properties.name = null;
        properties.rootDirectory = null;
        properties.buildTarget = null;
        Workspace workspace = Evergreen.getInstance().getCurrentWorkspace();
        if (workspace != null) {
            properties.buildTarget = workspace.getBuildTarget();
            ETextWindow textWindow = ETextAction.getFocusedTextWindow();
            if (textWindow != null) {
                String friendlyDirectory = FileUtilities.getUserFriendlyName(FileUtilities.fileFromString(textWindow.getFilename()).getParent());
                properties.rootDirectory = friendlyDirectory;
                if (friendlyDirectory.startsWith(workspace.getRootDirectory())) {
                    int prefixCharsToSkip = workspace.getRootDirectory().length();
                    String pathWithinWorkspace = friendlyDirectory.substring(prefixCharsToSkip);
                    // When adding a workspace which overlaps an existing one, choose a name that will sort close to but after the current workspace.
                    properties.name = workspace.getWorkspaceName() + " " + pathWithinWorkspace;
                }
            }
        }
        
        if (properties.showWorkspacePropertiesDialog("New Workspace", "Create") == true) {
            Evergreen.getInstance().createWorkspace(properties);
        }
    }
}
