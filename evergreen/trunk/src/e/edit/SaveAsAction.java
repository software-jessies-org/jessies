package e.edit;

import e.gui.*;
import java.awt.event.*;

public class SaveAsAction extends ETextAction {
    public SaveAsAction() {
        super("Save As...");
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("S", true));
        GnomeStockIcon.configureAction(this);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        String filename = Evergreen.getInstance().getCurrentWorkspace().showSaveAsDialog();
        if (filename != null) {
            window.saveAs(filename);
        }
    }
}
