package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class OpenAction extends AbstractAction {
    public static final String ACTION_NAME = "Open...";
    
    public OpenAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        Edit.getCurrentWorkspace().showOpenDialog();
    }
}
