package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.event.*;

/**
 * Opens the "Open Quickly" dialog with the current selection entered in the dialog's
 * text field.
 */
public class OpenQuicklyAction extends ETextAction {
    public OpenQuicklyAction() {
        super("Open _Quickly...", GuiUtilities.makeKeyStroke("O", false));
        GnomeStockIcon.useStockIcon(this, "gtk-open");
    }
    
    public void actionPerformed(ActionEvent e) {
        String filename = getSelectedText();
        if (filename.startsWith("~") || filename.startsWith("/")) {
            // If we have an absolute name, we can go straight there.
            Evergreen.getInstance().openFile(filename);
        } else {
            Evergreen.getInstance().getCurrentWorkspace().showOpenQuicklyDialog(StringUtilities.regularExpressionFromLiteral(filename));
        }
    }
}
