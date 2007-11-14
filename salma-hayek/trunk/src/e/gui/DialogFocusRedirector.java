package e.gui;

import java.awt.*;
import javax.swing.*;

public class DialogFocusRedirector {
    private Container ui;
    private Component originalFocusOwner;
    
    public DialogFocusRedirector(Container ui) {
        this.ui = ui;
    }
    
    public void redirectFocus() {
        originalFocusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        giveFocusToFirstTextComponent();
    }
    
    public void restoreFocus() {
        if (originalFocusOwner != null) {
            originalFocusOwner.requestFocus();
        }
    }
    
    public boolean isWorthGivingFocusTo(Component c) {
        return (c instanceof ETextField || c instanceof JTextField);
    }
    
    private boolean giveFocusToFirstTextComponent() {
        Component[] components = ui.getComponents();
        return giveFocusToFirstTextComponentIn(components);
    }
    
    private boolean giveFocusToFirstTextComponentIn(Component[] components) {
        for (Component component : components) {
            if (isWorthGivingFocusTo(component)) {
                component.requestFocus();
                ((JTextField) component).selectAll();
                return true;
            } else if (component instanceof Container) {
                Component[] newComponents = ((Container) component).getComponents();
                boolean focusGivenAway = giveFocusToFirstTextComponentIn(newComponents);
                if (focusGivenAway) return true;
            }
        }
        return false;
    }
}
