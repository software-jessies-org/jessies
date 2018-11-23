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
        // If we have an absolute name, we can go straight there.
        // Avoid selections with newlines in: any selection that starts with a comment would look like a filename!
        // We also only *try* to open the file. If no such file exists, we swallow the exception and
        // fall back to showing the 'Open Quickly' dialog.
        if (filename.indexOf('\n') == -1 && (filename.startsWith("~") || filename.startsWith("/"))) {
            try {
                Evergreen.getInstance().openFileNonInteractively(filename);
                return;
            } catch (RuntimeException ex) {}
        }
        Evergreen.getInstance().getCurrentWorkspace().showOpenQuicklyDialog(ETextAction.getSelectedRegularExpression());
    }
}
