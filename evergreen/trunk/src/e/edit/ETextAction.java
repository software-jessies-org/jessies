package e.edit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

public abstract class ETextAction extends TextAction {
    public ETextAction(String name) {
        super(name);
    }
    
    public abstract void actionPerformed(ActionEvent e);

    /**
     * Use this unless you really need an ETextArea or ETextWindow.
     * If you don't, your functionality won't be available on the errors
     * window, for instance.
     */
    public JTextComponent getTextComponent() {
        Component component = getFocusedComponent();
        if (component instanceof JTextComponent) {
            return (JTextComponent) component;
        }
        return null;
    }

    public ETextArea getTextArea() {
        Component component = getFocusedComponent();
        if (component instanceof ETextArea) {
            return (ETextArea) component;
        }
        if (component instanceof EWindow) {
            return ((EWindow) component).getText();
        }
        return (ETextArea) (SwingUtilities.getAncestorOfClass(ETextArea.class, component));
    }
    
    public ETextWindow getFocusedTextWindow() {
        Component focusedComponent = getFocusedComponent();
        if (focusedComponent instanceof ETextArea == false) {
            return null;
        }
        ETextArea target = (ETextArea) focusedComponent;
        ETextWindow textWindow = (ETextWindow) SwingUtilities.getAncestorOfClass(ETextWindow.class, target);
        return textWindow;
    }
    
    public String getSelectedText() {
        JTextComponent textComponent = getTextComponent();
        return (textComponent != null) ? textComponent.getSelectedText() : "";
    }
}
