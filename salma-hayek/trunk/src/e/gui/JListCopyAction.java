package e.gui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * JList has the InputMap side of control-C copying; this is the ActionMap side.
 * The selected items are converted to strings and concatenated. The result
 * is copied to the clipboard.
 */
public class JListCopyAction extends AbstractAction {
    /**
     * Fixes the ActionMap for the given JList.
     */
    public static void fixCopyFor(JList list) {
        list.getActionMap().put("copy", new JListCopyAction());
    }
    
    public JListCopyAction() {
        super("copy");
    }
    
    public void actionPerformed(ActionEvent e) {
        copyListSelectionToClipboard((JList) e.getSource());
    }
    
    private void copyListSelectionToClipboard(JList list) {
        // Make a StringSelection corresponding to the selected lines.
        StringBuilder buffer = new StringBuilder();
        Object[] selectedLines = list.getSelectedValues();
        for (int i = 0; i < selectedLines.length; ++i) {
            String line = selectedLines[i].toString();
            buffer.append(line);
            buffer.append('\n');
        }
        StringSelection selection = new StringSelection(buffer.toString());
        
        // Set the clipboard (and X11's nasty hacky semi-duplicate).
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        toolkit.getSystemClipboard().setContents(selection, selection);
        if (toolkit.getSystemSelection() != null) {
            toolkit.getSystemSelection().setContents(selection, selection);
        }
    }
}
