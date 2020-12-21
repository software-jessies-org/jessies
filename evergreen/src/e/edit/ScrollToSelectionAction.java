package e.edit;

import e.ptextarea.*;

/**
 * An action that ensures that the selection is visible.
 */
public class ScrollToSelectionAction extends PTextAction {
    public ScrollToSelectionAction() {
        super("Scroll to _Selection", "J", false);
    }
    
    public void performOn(PTextArea textArea) {
        int offset = textArea.getUnanchoredSelectionExtreme();
        textArea.centerOffsetInDisplay(offset);
        textArea.brieflyShowBigRedArrowPointingAt(offset);
    }
}
