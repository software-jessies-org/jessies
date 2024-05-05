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
        if (GuiUtilities.isMacOs()) {
            String key = (indexDelta == -1) ? "OPEN_BRACKET" : "CLOSE_BRACKET";
            putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStroke(key, true));
        } else {
            // [philjessies, 2024-05-05] Use alt+left/right to switch workspaces on non-Macs.
            // The bracket keys seem unnatural, and Chrome's ctrl+tab/ctrl+shift+tab is torture.
            // We use alt+left/right in Terminator, and it seems like a natural use of arrow keys.
            String key = (indexDelta == -1) ? "LEFT" : "RIGHT";
            putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStrokeWithModifiers(InputEvent.ALT_DOWN_MASK, key));
        }
    }
    
    @Override public boolean isEnabled() {
        return !Evergreen.getInstance().getWorkspaces().isEmpty();
    }
    
    public void actionPerformed(ActionEvent e) {
        Evergreen.getInstance().cycleWorkspaces(indexDelta);
    }
}
