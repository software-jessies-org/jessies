package e.edit;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
The ETextArea action to find the previous match.
*/
public class FindPreviousAction extends ETextAction {
    public static final String ACTION_NAME = "Find Previous";

    public FindPreviousAction() {
        super(ACTION_NAME);
    }
    
    public boolean isEnabled() {
        return super.isEnabled() && (getFocusedTextWindow() != null);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow textWindow = getFocusedTextWindow();
        if (textWindow != null) {
            textWindow.findPrevious();
        }
    }
}
