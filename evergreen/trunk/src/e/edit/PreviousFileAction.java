package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class PreviousFileAction extends AbstractAction {
    public static final String ACTION_NAME = "Previous File";
    
    public PreviousFileAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, EditMenuBar.makeKeyStroke("BACK_QUOTE", true));
    }
    
    public void actionPerformed(ActionEvent e) {
        Edit.getCurrentWorkspace().switchToPreviousFile();
    }
}
