package e.edit;

import java.awt.event.*;

/**
The ETextArea save action.
*/
public class SaveAction extends ETextAction {
    public static final String ACTION_NAME = "Save";

    public SaveAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("S", false));
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        if (window == null) {
            return;
        }
        window.save();
    }
}
