package e.edit;

import e.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Moves the currently-selected window up or down in the column.
 */
public class MoveWindowAction extends ETextAction {
    private int indexDelta;
    
    public MoveWindowAction(int indexDelta) {
        super((indexDelta == -1) ? "Move Window _Up" : "Move Window _Down", null);
        this.indexDelta = indexDelta;
        initKeyboardEquivalent();
    }
    
    private void initKeyboardEquivalent() {
        // [philjessies, 2025-05-04]
        // The CycleWindowsAction has special keys for MacOS, but I don't know what those
        // would be for _this_ functionality. I'll just use shift+alt to be consistent
        // with the non-Mac keys we use for the CycleWindowsAction; if someone knows what
        // this should be on MacOS, please add that using the normal GuiUtilities.isMacOs()
        // condition.
        String key = (indexDelta == -1) ? "UP" : "DOWN";
        putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStrokeWithModifiers(InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, key));
    }
    
    @Override public boolean isEnabled() {
        if (!super.isEnabled()) {
            return false;
        }
        EWindow window = (EWindow) SwingUtilities.getAncestorOfClass(EWindow.class, getFocusedComponent());
        if (window == null) {
            return false;
        }
        EColumn column = window.getColumn();
        if (column == null) {
            return false;
        }
        return column.canMoveWindow(window, indexDelta);
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
        column.moveWindow(window, indexDelta);
    }
}
