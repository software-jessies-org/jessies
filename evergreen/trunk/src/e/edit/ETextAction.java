package e.edit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import e.gui.*;

public abstract class ETextAction extends TextAction {
    public ETextAction(String name) {
        super(name);
    }
    
    public abstract void actionPerformed(ActionEvent e);

    public ETextWindow getFocusedTextWindow() {
        Component focusedComponent = getFocusedComponent();
        if (focusedComponent instanceof ETextArea == false) {
            return null;
        }
        ETextArea target = (ETextArea) focusedComponent;
        ETextWindow textWindow = (ETextWindow) SwingUtilities.getAncestorOfClass(ETextWindow.class, target);
        return textWindow;
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
}
