package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class KillErrorsAction extends AbstractAction {
    public static final String ACTION_NAME = "Clear Errors";
    
    public KillErrorsAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        Edit.getCurrentWorkspace().getErrorsWindow().clear();
    }

    public boolean isEnabled() {
        Workspace workspace = Edit.getCurrentWorkspace();
        return super.isEnabled() && workspace != null;
    }
}
