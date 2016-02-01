package e.edit;

import e.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Moves the focus a given number of workspaces forward or back.
 */
public class CycleWorkspacesAction extends AbstractAction {
    private int indexDelta;
    
    public CycleWorkspacesAction(int indexDelta) {
        GuiUtilities.configureAction(this, (indexDelta == 1) ? "_Next Workspace" : "_Previous Workspace", null);
        this.indexDelta = indexDelta;
        initKeyboardEquivalent();
    }
    
    private void initKeyboardEquivalent() {
        String key = "TAB";
        boolean shifted = (indexDelta == -1);
        
        boolean tabDoesNotWork = true;
        if (tabDoesNotWork || GuiUtilities.isMacOs()) {
            key = (indexDelta == -1) ? "OPEN_BRACKET" : "CLOSE_BRACKET";
            shifted = true;
        }
        
        putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStroke(key, shifted));
    }
    
    @Override public boolean isEnabled() {
        return !Evergreen.getInstance().getWorkspaces().isEmpty();
    }
    
    public void actionPerformed(ActionEvent e) {
        Evergreen.getInstance().cycleWorkspaces(indexDelta);
    }
}
