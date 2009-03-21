package e.edit;

import java.awt.event.*;
import javax.swing.*;

/**
 * The user is expected to use Open Quickly, so this is only
 * here for emergencies. That's why, for example, there's no
 * keyboard equivalent.
 */
public class OpenAction extends AbstractAction {
    public OpenAction() {
        super("Open...");
    }
    
    @Override public boolean isEnabled() {
        return !Evergreen.getInstance().getWorkspaces().isEmpty();
    }
    
    public void actionPerformed(ActionEvent e) {
        Evergreen.getInstance().getCurrentWorkspace().showOpenDialog();
    }
}
