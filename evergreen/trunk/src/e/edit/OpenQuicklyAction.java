package e.edit;

import java.awt.event.*;

/**
 * Opens the "Open Quickly" dialog with the current selection entered in the dialog's
 * text field.
 */
public class OpenQuicklyAction extends ETextAction {
    public OpenQuicklyAction() {
        super("Open Quickly...");
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("O", false));
    }
    
    public void actionPerformed(ActionEvent e) {
        String filename = getSelectedText();
        if (filename.startsWith("~") || filename.startsWith("/")) {
            // If we have an absolute name, we can go straight there.
            Edit.getInstance().openFile(filename);
        } else {
            Edit.getInstance().getCurrentWorkspace().showOpenQuicklyDialog(filename);
        }
    }
}
