package e.edit;

import e.ptextarea.*;
import javax.swing.*;

/**
 * A text-editing component.
 */
public class ETextArea extends PTextArea {
    public String reformatPastedText(String pastedText) {
        return pastedText.replace('\u00a0', ' ');
    }
    
    //
    // Work round PTextArea's unfinished find functionality.
    //
    
    protected void findNextOrPrevious(boolean next) {
        updateFindResults();
        super.findNextOrPrevious(next);
    }
    
    public void updateFindResults() {
        ETextWindow textWindow = (ETextWindow) SwingUtilities.getAncestorOfClass(ETextWindow.class, this);
        FindAction.INSTANCE.repeatLastFind(textWindow);
    }
}
