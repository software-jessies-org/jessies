package e.edit;

import javax.swing.text.*;

/**
 * Opens the "Open Quickly" dialog with the current selection entered in the dialog's
 * text field.
 */
public class OpenSelectionAction extends SelectedTextAction {
    public OpenSelectionAction() {
        super("Open Quickly...");
    }
    
    public void actOnSelection(JTextComponent component, String selection) {
        Edit.getCurrentWorkspace().showOpenQuicklyDialog(selection);
    }
}
