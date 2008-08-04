package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.event.*;

public class SaveAsAction extends ETextAction {
    public SaveAsAction() {
        super("Save _As...", GuiUtilities.makeKeyStroke("S", true));
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
