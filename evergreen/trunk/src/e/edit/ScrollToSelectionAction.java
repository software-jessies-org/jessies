package e.edit;

import java.awt.event.*;

/**
 * An action that ensures that the selection is visible.
 */
public class ScrollToSelectionAction extends ETextAction {
    public static final String ACTION_NAME = "Scroll to Selection";

    public ScrollToSelectionAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("J", false));
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextArea area = getTextArea();
        if (area == null) {
            return;
        }
        area.ensureVisibilityOfOffset(area.getUnanchoredSelectionExtreme());
    }
}
