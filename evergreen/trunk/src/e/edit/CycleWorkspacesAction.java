package e.edit;

import e.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Moves the focus a given number of workspaces forward or back.
 */
public class CycleWorkspacesAction extends ETextAction {
    private int indexDelta;
    
    public CycleWorkspacesAction(int indexDelta) {
        super((indexDelta == 1) ? "Next Workspace" : "Previous Workspace");
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
    
    public void actionPerformed(ActionEvent e) {
        Edit.cycleWorkspaces(indexDelta);
    }
}
