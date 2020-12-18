package e.edit;

import e.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Creates a workspace pointing at the Evergreen configuration directory.
 */
public class NewConfigWorkspaceAction extends AbstractAction {
    public NewConfigWorkspaceAction() {
        GuiUtilities.configureAction(this, "Evergreen Config Workspace", null);
    }
    
    public void actionPerformed(ActionEvent e) {
        String dir = Evergreen.getPreferenceDir();
        // Do we have the config workspace open already? If so, just select it.
        // Note that the Workspace creation actually mutates the given path, turning it into a
        // "user-friendly" name. So we do the same to 'dir', and just for good measure we also
        // do it again to the workspace root (in case its action changes in the future).
        String friendlyDir = FileUtilities.getUserFriendlyName(dir);
        for (Workspace ws : Evergreen.getInstance().getWorkspaces()) {
            if (FileUtilities.getUserFriendlyName(ws.getRootDirectory()).equals(friendlyDir)) {
                Evergreen.getInstance().selectWorkspace(ws);
                return;
            }
        }
        // Not already open, so create it.
        WorkspaceProperties properties = new WorkspaceProperties();
        properties.name = "EvergreenConfig";
        properties.rootDirectory = dir;
        properties.buildTarget = null;
        Evergreen.getInstance().createWorkspace(properties);
    }
}
