package e.gui;

import java.awt.event.*;

/**
 * Useful to avoid conditional code in places where you have 0 or 1 listeners.
 */
public final class NoOpAction implements ActionListener {
    /**
     * Does nothing.
     */
    public void actionPerformed(ActionEvent e) {
    }
}
