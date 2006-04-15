package e.edit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public abstract class ETextAction extends AbstractAction {
    public ETextAction(String name) {
        super(name);
    }
    
    public abstract void actionPerformed(ActionEvent e);

    public static Component getFocusedComponent() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
    }
    
    public static ETextArea getFocusedTextArea() {
        Component focusedComponent = getFocusedComponent();
        if (focusedComponent instanceof ETextArea) {
            return (ETextArea) focusedComponent;
        }
        return (ETextArea) SwingUtilities.getAncestorOfClass(ETextArea.class, focusedComponent);
    }
    
    public static ETextWindow getFocusedTextWindow() {
        Component focusedComponent = getFocusedComponent();
        if (focusedComponent instanceof ETextArea == false) {
            return null;
        }
        ETextArea target = (ETextArea) focusedComponent;
        ETextWindow textWindow = (ETextWindow) SwingUtilities.getAncestorOfClass(ETextWindow.class, target);
        return textWindow;
    }
    
    public String getSelectedText() {
        return (getFocusedTextArea() != null) ? getFocusedTextArea().getSelectedText() : "";
    }
}
