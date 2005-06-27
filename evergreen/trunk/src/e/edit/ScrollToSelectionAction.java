package e.edit;

import e.ptextarea.*;

/**
 * An action that ensures that the selection is visible.
 */
public class ScrollToSelectionAction extends PTextAction {
    public ScrollToSelectionAction() {
        super("Scroll to Selection", e.util.GuiUtilities.makeKeyStroke("J", false));
    }
    
    public void performOn(PTextArea textArea) {
        textArea.centerOffsetInDisplay(textArea.getUnanchoredSelectionExtreme());
    }
}
