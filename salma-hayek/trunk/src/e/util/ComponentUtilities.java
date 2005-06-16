package e.util;

import java.awt.*;
import java.util.*;

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
    
    private ComponentUtilities() { /* Not instantiable. */ }
}
