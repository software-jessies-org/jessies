package e.edit;

import java.awt.event.*;

/**
 * Opens the "Open Quickly" dialog with the current selection entered in the dialog's
 * text field.
 */
public class OpenSelectionAction extends ETextAction {
    public OpenSelectionAction() {
        super("Open Quickly...");
        putValue(ACCELERATOR_KEY, EditMenuBar.makeKeyStroke("D", true));
    }
    
    public void actionPerformed(ActionEvent e) {
        Edit.getCurrentWorkspace().showOpenQuicklyDialog(getSelectedText());
    }
}
