package e.edit;

import e.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Switches to the counterpart of the current file (for example, the .h file for a .cpp or vice versa).
 */
public class ShowCounterpartAction extends ETextAction {
    public static final KeyStroke KEYSTROKE = GuiUtilities.makeKeyStroke("P", true);
    
    public ShowCounterpartAction() {
        super("_Switch to Header/Source", KEYSTROKE);
    }
    
    @Override public boolean isEnabled() {
        final ETextWindow textWindow = getFocusedTextWindow();
        return (textWindow != null && textWindow.getCounterpartFilename() != null);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow textWindow = getFocusedTextWindow();
        if (textWindow != null) {
            textWindow.switchToCounterpart();
        }
    }
}
