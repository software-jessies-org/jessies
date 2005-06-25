package e.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class DialogFocusRedirector {
    private Container ui;
    private Component originalFocusOwner;
    
    public DialogFocusRedirector(Container ui) {
        this.ui = ui;
    }
    
    public void redirectFocus() {
        originalFocusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        Component[] components = ui.getComponents();
        giveFocusToFirstTextComponentIn(components);
    }
    
    public void restoreFocus() {
        if (originalFocusOwner != null) {
            originalFocusOwner.requestFocus();
        }
    }
    
    public boolean isWorthGivingFocusTo(Component c) {
        return (c instanceof ETextField || c instanceof JTextField);
    }
    
    public boolean giveFocusToFirstTextComponentIn(Component[] components) {
        for (Component component : components) {
            if (isWorthGivingFocusTo(component)) {
                component.requestFocus();
                JTextField.class.cast(component).selectAll();
                return true;
            } else if (component instanceof Container) {
                Component[] newComponents = Container.class.cast(component).getComponents();
                boolean focusGivenAway = giveFocusToFirstTextComponentIn(newComponents);
                if (focusGivenAway) return true;
            }
        }
        return false;
    }
}
