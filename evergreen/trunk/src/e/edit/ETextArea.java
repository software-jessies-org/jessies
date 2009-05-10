package e.edit;

import e.ptextarea.*;
import javax.swing.*;

/**
 * A text-editing component.
 */
public class ETextArea extends PTextArea {
    public ETextArea() {
    }
    
    @Override protected void updateFindResults() {
        ETextWindow textWindow = (ETextWindow) SwingUtilities.getAncestorOfClass(ETextWindow.class, this);
        FindAction.INSTANCE.repeatLastFind(textWindow);
    }
}
