package e.edit;

import e.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Switches to the test file for the current filename.
 */
public class ShowTestFileAction extends ETextAction {
    public static final KeyStroke KEYSTROKE = GuiUtilities.makeKeyStroke("T", true);
    
    public ShowTestFileAction() {
        super("Switch to _Test", KEYSTROKE);
    }
    
    @Override public boolean isEnabled() {
        final ETextWindow textWindow = getFocusedTextWindow();
        return (textWindow != null && textWindow.getTestFilename() != null);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow textWindow = getFocusedTextWindow();
        if (textWindow != null) {
            textWindow.switchToTestFile();
        }
    }
}
