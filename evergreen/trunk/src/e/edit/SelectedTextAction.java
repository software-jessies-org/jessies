package e.edit;

import java.awt.event.*;
import javax.swing.text.*;

/**
 * Subclass this if you have an action that should take place on the current selection in a text.
 */
public abstract class SelectedTextAction extends TextAction {
    /** Override this with your behavior. 'selection' is guaranteed to be non-null and non-zero length. */
    public abstract void actOnSelection(JTextComponent component, String selection);
    
    public SelectedTextAction(String name) {
        super(name);
    }
    
    /** Passes the selection to actOnSelection, if there's some text available. */
    public void actionPerformed(ActionEvent e) {
        JTextComponent component = getFocusedComponent();
        if (component == null) {
            return;
        }
        
        String selection = component.getSelectedText().trim();
        if (selection.length() > 0) {
            actOnSelection(component, selection);
        }
    }
    
    /** Ensures that we're not enabled if there's no suitable selection. */
    public boolean isEnabled() {
        if (super.isEnabled() == false) {
            return false;
        }
        String selection = getFocusedComponent().getSelectedText();
        if (selection == null || selection.length() == 0) {
            return false;
        }
        return true;
    }
}
