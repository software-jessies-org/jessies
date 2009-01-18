package e.edit;

import e.util.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

/**
 * Opens a dialog where the user can specify the details of a new workspace, which will then be created.
 */
public class AddWorkspaceAction extends AbstractAction {
    public AddWorkspaceAction() {
        GuiUtilities.configureAction(this, "_Add Workspace...", null);
    }
    
    public void actionPerformed(ActionEvent e) {
        WorkspaceProperties properties = new WorkspaceProperties();
        properties.name = "";
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
                    properties.name = workspace.getTitle() + " " + pathWithinWorkspace;
                } else {
                    // Where the file prompting the workspace addition is from elsewhere, the best name is likely to be just one component but deleting is less work than typing.
                    properties.name = friendlyDirectory;
                }
            }
        }
        
        if (properties.showWorkspacePropertiesDialog("Add Workspace", "Add") == true) {
            Evergreen.getInstance().createWorkspace(properties);
        }
    }
}
