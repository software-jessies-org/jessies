package e.edit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class FindFilesAction extends AbstractAction {
    public static final String ACTION_NAME = "Find in Files...";
    
    public FindFilesAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        Edit.getCurrentWorkspace().showFindFilesDialog(null, null);
    }
}
