package e.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class DialogFocusRedirector implements FocusListener {
    private Container ui;
    
    public DialogFocusRedirector(Container ui) {
        this.ui = ui;
    }
    
    /**
    * Invoked when our dialog gains the focus; gives the focus to the first
    * text component it finds.
    */
    public void focusGained(FocusEvent e) {
        redirectFocus();
    }
    
    /* Ignores focus-lost events. */
    public void focusLost(FocusEvent e) {
    }
    
    public void redirectFocus() {
        Component[] components = ui.getComponents();
        giveFocusToFirstTextComponentIn(components);
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
