package e.edit;

import java.awt.event.*;

/**
The ETextArea action to find the next match.
*/
public class FindNextAction extends ETextAction {
    public static final String ACTION_NAME = "Find Next";

    public FindNextAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow textWindow = getFocusedTextWindow();
        if (textWindow != null) {
            textWindow.findNext();
        }
    }
}
