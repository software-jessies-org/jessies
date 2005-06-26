package e.ptextarea;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public abstract class PTextAction extends AbstractAction {
    private PTextArea boundTextArea;
    
    /**
     * The parameter keyStroke can be null if you don't want to bind this
     * action to a key.
     */
    public PTextAction(String name, KeyStroke keyStroke) {
        super(name);
        if (keyStroke != null) {
            putValue(ACCELERATOR_KEY, keyStroke);
        }
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
        Component component = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (component instanceof PTextArea) {
            return (PTextArea) component;
        }
        return null;
    }
}
