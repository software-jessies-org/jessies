package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class RescanWorkspaceAction extends AbstractAction {
    public static final String ACTION_NAME = "Rescan Files";
    
    public RescanWorkspaceAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        Edit.getInstance().getCurrentWorkspace().updateFileList(null);
    }
}
