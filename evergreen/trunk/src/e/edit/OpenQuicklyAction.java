package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Opens the "Open Quickly" dialog with the current selection (if any) entered in the dialog's text field.
 */
public class OpenQuicklyAction extends AbstractAction {
    public OpenQuicklyAction() {
        GuiUtilities.configureAction(this, "_Open Quickly...", GuiUtilities.makeKeyStroke("O", false));
        GnomeStockIcon.useStockIcon(this, "gtk-open");
    }
    
    @Override public boolean isEnabled() {
        return !Evergreen.getInstance().getWorkspaces().isEmpty();
    }
    
    public void actionPerformed(ActionEvent e) {
        final String filename = ETextAction.getSelectedText();
        if (filename.startsWith("~") || filename.startsWith("/")) {
            // If we have an absolute name, we can go straight there.
            Evergreen.getInstance().openFile(filename);
            return;
        }
        Evergreen.getInstance().getCurrentWorkspace().showOpenQuicklyDialog(ETextAction.getSelectedRegularExpression());
    }
}
