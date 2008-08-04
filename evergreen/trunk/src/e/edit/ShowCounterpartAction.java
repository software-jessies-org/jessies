package e.edit;

import e.util.*;
import java.awt.event.*;

/**
 * Switches to the counterpart of the current file (for example, the .h file for a .cpp or vice versa).
 */
public class ShowCounterpartAction extends ETextAction {
    public ShowCounterpartAction() {
        super("_Switch to Header/Source", GuiUtilities.makeKeyStroke("P", true));
    }
    
    public boolean isEnabled() {
        ETextWindow textWindow = getFocusedTextWindow();
        return (textWindow != null && textWindow.getCounterpartFilename() != null);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow textWindow = getFocusedTextWindow();
        if (textWindow != null) {
            textWindow.switchToCounterpart();
        }
    }
}
