package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class NextFileAction extends AbstractAction {
    public static final String ACTION_NAME = "Next File";
    
    public NextFileAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        Edit.getCurrentWorkspace().switchToNextFile();
    }
}
