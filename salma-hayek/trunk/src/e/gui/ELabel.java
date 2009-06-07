package e.gui;

import javax.swing.*;

/**
 * A JLabel without the broken empty-string behavior.
 */
public class ELabel extends JLabel {
    public ELabel() {
        super(" ");
    }
    
    /**
     * Sets the text shown by this label.
     * The empty string is substituted by a single space to avoid a Swing misfeature where a JLabel set to the empty string is zero-height.
     * In the unlikely event you want that behavior, use JLabel directly instead.
     */
    @Override public void setText(String newText) {
        if (newText == null || newText.length() == 0) {
            newText = " ";
        }
        
        super.setText(newText);
    }
}
