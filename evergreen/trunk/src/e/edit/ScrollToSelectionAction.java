package e.edit;

import java.awt.event.*;
import e.gui.*;

/**
An action that ensures that the selection is visible.
*/
public class ScrollToSelectionAction extends ETextAction {
    public static final String ACTION_NAME = "Scroll to Selection";

    public ScrollToSelectionAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("J", false));
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        if (window == null) {
            return;
        }
        JTextComponentUtilities.ensureVisibilityOfOffset(window.getText(), window.getText().getCaretPosition());
    }
}
