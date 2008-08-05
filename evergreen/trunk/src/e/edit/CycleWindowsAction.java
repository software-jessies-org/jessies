package e.edit;

import e.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Moves the focus a given number of windows forward or back.
 */
public class CycleWindowsAction extends ETextAction {
    private int indexDelta;
    
    public CycleWindowsAction(int indexDelta) {
        super((indexDelta == 1) ? "_Next Window" : "_Previous Window", null);
        this.indexDelta = indexDelta;
        initKeyboardEquivalent();
    }
    
    private void initKeyboardEquivalent() {
        // Microsoft says that "Next Window" should be either alt-F6 or
        // ctrl-F6, depending on what kind of window you think we're cycling
        // through. They don't specify any key for "Previous Window".
        // You know what (shift-)alt-tab does, and (shift-)ctrl-tab is used
        // to cycle between tabs (which are workspaces here).
        
        // GNOME doesn't appear to support keyboard traversal in either
        // direction, presumably because they don't support any kind of MDI
        // other than tabs (which we use for workspaces, not windows within
        // a workspace).
        
        // So, let's pretend everything's Mac OS...
        String key = "BACK_QUOTE";
        boolean shifted = (indexDelta == -1);
        putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStroke(key, shifted));
    }
    
    public void actionPerformed(ActionEvent e) {
        EWindow window = (EWindow) SwingUtilities.getAncestorOfClass(EWindow.class, getFocusedComponent());
        if (window == null) {
            return;
        }
        EColumn column = window.getColumn();
        if (column == null) {
            return;
        }
        column.cycleWindow(window, indexDelta);
    }
}
