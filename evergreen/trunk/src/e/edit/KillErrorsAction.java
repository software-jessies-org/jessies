package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class KillErrorsAction extends AbstractAction {
    public static final String ACTION_NAME = "Clear Errors";
    
    public KillErrorsAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        Workspace workspace = Edit.getCurrentWorkspace();
        if (workspace == null) {
            return;
        }
        EErrorsWindow errorsWindow = workspace.getErrorsWindow();
        if (errorsWindow == null) {
            return;
        }
        errorsWindow.clear();
    }
}
