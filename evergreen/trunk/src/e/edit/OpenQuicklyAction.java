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
        Edit.getInstance().getCurrentWorkspace().showOpenQuicklyDialog(getSelectedText());
    }
}
