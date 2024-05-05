package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Reopens the last file to be closed in the current workspace.
 * This is similar to how ctrl+shift+t works in Chrome, except that we restrict which file is
 * reopened to be within the current workspace, which is probably closer to what the user expects.
 */
public class ReopenLastClosedAction extends AbstractAction {
    public ReopenLastClosedAction() {
        GuiUtilities.configureAction(this, "R_eopen last-closed file", GuiUtilities.makeKeyStroke("T", true));
    }
    
    @Override public boolean isEnabled() {
        Workspace ws = Evergreen.getInstance().getCurrentWorkspace();
        return ws != null && !ws.getRecentlyClosedFiles().isEmpty();
    }
    
    public void actionPerformed(ActionEvent e) {
        Workspace ws = Evergreen.getInstance().getCurrentWorkspace();
        if (ws != null) {
            // We use Evergreen.openFile here, because it understands the 'address' part of the
            // filename and opens it at the right place. However, it will pick the workspace to open the file
            // in according to where it thinks it should go. This will nearly always be the current
            // workspace, but it's _possible_ for it to be a different one. This would lead to an
            // entry in the current workspace's recently closed files list which can't be removed.
            // If this ever actually happens in real life (and isn't anything more than a slight
            // irritation, once), then fix this :-)
            ArrayList<String> closed = ws.getRecentlyClosedFiles();
            if (!closed.isEmpty()) {
                Evergreen.getInstance().openFile(closed.get(0));
            }
        }
    }
}
