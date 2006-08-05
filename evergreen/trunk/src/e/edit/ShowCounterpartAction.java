package e.edit;

import java.awt.event.*;
import java.lang.reflect.*;
import java.util.regex.*;

/**
 * Switches to the counterpart of the current file (for example, the .h file for a .cpp or vice versa).
 */
public class ShowCounterpartAction extends ETextAction {
    public static final String ACTION_NAME = "Switch to Header/Source";
    
    public ShowCounterpartAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("P", true));
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
