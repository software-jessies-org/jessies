package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class OpenQuicklyAction extends AbstractAction {
    public static final String ACTION_NAME = "Open Quickly...";
    
    public OpenQuicklyAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        Edit.getCurrentWorkspace().showOpenQuicklyDialog("");
    }
}
