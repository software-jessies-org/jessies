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
        return (c instanceof JTextField);
    }
    
    private boolean giveFocusToFirstTextComponent() {
        Component[] components = ui.getComponents();
        return giveFocusToFirstTextComponentIn(components);
    }
    
    private boolean giveFocusToFirstTextComponentIn(Component[] components) {
        for (Component component : components) {
            if (isWorthGivingFocusTo(component)) {
                component.requestFocus();
                // If there's no selection, select all so the user can conveniently replace everything.
                // If there is a selection, assume that's because the dialog's creator has done something clever.
                // FIXME: this seems okay for now, but doesn't let us implement a dialog whose first text field should always be appended to. If the need ever arises, use a client property on the JTextField to signal we don't want the selectAll?
                JTextField textField = (JTextField) component;
                if (textField.getSelectionStart() == textField.getSelectionEnd()) {
                    textField.selectAll();
                }
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
