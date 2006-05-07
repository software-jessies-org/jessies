package e.edit;

import java.awt.event.*;
import javax.swing.*;

/**
 * The user is expected to use Open Quickly, so this is only
 * here for emergencies. That's why, for example, there's no
 * keyboard equivalent.
 */
public class OpenAction extends AbstractAction {
    public static final String ACTION_NAME = "Open...";
    
    public OpenAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        Evergreen.getInstance().getCurrentWorkspace().showOpenDialog();
    }
}
