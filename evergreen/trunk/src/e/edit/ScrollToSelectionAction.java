package e.edit;

import java.awt.event.*;

/**
An action that ensures that the selection is visible.
*/
public class ScrollToSelectionAction extends ETextAction {
    public static final String ACTION_NAME = "Scroll to Selection";

    public ScrollToSelectionAction() {
        super(ACTION_NAME);
    }
    
    public boolean isEnabled() {
        return super.isEnabled() && (getFocusedTextWindow() != null);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        window.getText().ensureVisibilityOfOffset(window.getText().getCaretPosition());
    }
}
