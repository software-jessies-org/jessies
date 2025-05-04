package e.edit;

import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;

/**
 * Dumps the current state of the focused text area's undo buffer to the warning log.
 */
public class DumpUndoBufferAction extends ETextAction {
    public DumpUndoBufferAction() {
        super("Dump Undo Buffer", null);
        putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStrokeWithModifiers(InputEvent.ALT_DOWN_MASK, "Z"));
    }
    
    @Override public boolean isEnabled() {
        return getFocusedTextArea() != null;
    }
    
    public void actionPerformed(ActionEvent e) {
        PTextArea textArea = getFocusedTextArea();
        if (textArea == null) {
            return;
        }
        textArea.getTextBuffer().getUndoBuffer().dumpUndoList();
    }
}
