package e.util;

import java.awt.*;
import java.util.*;
import javax.swing.*;

public class ComponentUtilities {
    /**
     * Disables the forward and backward focus traversal keys on the given
     * component.
     */
    public static void disableFocusTraversal(Component c) {
        Set<AWTKeyStroke> emptySet = Collections.emptySet();
        c.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, emptySet);
        c.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, emptySet);
    }
    
    /**
     * Binds an Action to a JComponent via the Action's configured ACCELERATOR_KEY.
     */
    public static void initKeyBinding(JComponent component, Action action) {
        String name = (String) action.getValue(Action.NAME);
        KeyStroke keyStroke = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
        component.getActionMap().put(name, action);
        component.getInputMap().put(keyStroke, name);
    }
    
    private ComponentUtilities() { /* Not instantiable. */ }
}
