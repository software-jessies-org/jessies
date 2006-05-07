package e.edit;

import e.gui.*;
import java.awt.event.*;

public class SaveAsAction extends ETextAction {
    public static final String ACTION_NAME = "Save As...";
    
    public SaveAsAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("S", true));
        GnomeStockIcon.useStockIcon(this, "gtk-save-as");
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        String filename = Evergreen.getInstance().getCurrentWorkspace().showSaveAsDialog();
        if (filename != null) {
            window.saveAs(filename);
        }
    }
}
