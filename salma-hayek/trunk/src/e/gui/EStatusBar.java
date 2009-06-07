package e.gui;

import e.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A simple status bar. Works around the various problems you'd have if you used a JLabel directly.
 */
public class EStatusBar extends JLabel {
    /**
     * Constructs a status bar with no initial message.
     */
    public EStatusBar() {
        super(" ");
        
        // The default label fonts are a little large for status bars.
        if (System.getProperty("os.name").contains("Linux")) {
            setFont(UIManager.getFont("Menu.font"));
        } else if (GuiUtilities.isMacOs()) {
            setFont(UIManager.getFont("ToolTip.font"));
        }
    }
    
    /**
     * Sets the text to display in the status bar.
     */
    public void setText(String status) {
        // Works around a Swing misfeature whereby a JLabel showing the empty string is zero-height,
        // which is likely to mess up your layout. We substitute a single space for the empty string.
        if (status.length() == 0) {
            status = " ";
        }
        
        super.setText(status);
    }
}
