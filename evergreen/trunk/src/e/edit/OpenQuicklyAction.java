package e.edit;

import java.awt.event.*;

/**
 * Opens the "Open Quickly" dialog with the current selection entered in the dialog's
 * text field.
 */
public class OpenQuicklyAction extends ETextAction {
    public OpenQuicklyAction() {
        super("Open Quickly...");
        putValue(ACCELERATOR_KEY, EditMenuBar.makeKeyStroke("O", false));
    }
    
    public void actionPerformed(ActionEvent e) {
        Edit.getCurrentWorkspace().showOpenQuicklyDialog(getSelectedText());
    }
}
