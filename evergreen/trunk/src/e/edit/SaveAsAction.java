package e.edit;

import java.awt.event.*;

public class SaveAsAction extends ETextAction {
    public static final String ACTION_NAME = "Save As...";
    
    public SaveAsAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, EditMenuBar.makeKeyStroke("S", true));
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        String filename = Edit.getCurrentWorkspace().showSaveAsDialog();
        if (filename != null) {
            window.saveAs(filename);
        }
    }
}
