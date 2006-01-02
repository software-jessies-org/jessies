package e.gui;

import java.awt.event.*;
import javax.swing.*;

/**
 * Useful when you're building an application's menu but haven't implemented all the functionality yet.
 */
public final class PlaceholderAction extends AbstractAction {
    public PlaceholderAction(String name) {
        super(name);
    }
    
    /**
     * Does nothing.
     */
    public void actionPerformed(ActionEvent e) {
    }
    
    /**
     * Returns false because this action isn't implemented yet (or you wouldn't be using PlaceholderAction).
     */
    @Override
    public boolean isEnabled() {
        return false;
    }
}
