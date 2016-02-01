package e.ptextarea;

import e.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public abstract class PTextAction extends AbstractAction {
    private PTextArea boundTextArea;
    
    /**
     * The parameter 'key' can be null if you don't want to bind this action to a key, in which case 'shifted' is ignored.
     */
    public PTextAction(final String name, final String key, final boolean shifted) {
        GuiUtilities.configureAction(this, name, (key != null) ? GuiUtilities.makeKeyStroke(key, shifted) : null);
    }
    
    // If you don't require a focused text window, you probably shouldn't be a PTextAction.
    // Feel free to override if you have more specialist needs (but don't forget this check).
    public boolean isEnabled() {
        return (getTextArea() != null);
    }
    
    public void actionPerformed(ActionEvent e) {
        PTextArea textArea = getTextArea();
        if (textArea != null) {
            performOn(textArea);
        }
    }
    
    public abstract void performOn(PTextArea textArea);
    
    public void bindTo(PTextArea textArea) {
        this.boundTextArea = textArea;
    }
    
    protected PTextArea getTextArea() {
        if (boundTextArea != null) {
            return boundTextArea;
        }
        Component component = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        if (component instanceof PTextArea) {
            return (PTextArea) component;
        }
        return null;
    }
}
