package e.edit;

import java.awt.event.*;

public class OpenMakefileAction extends ETextAction {
    public static final String ACTION_NAME = "Open Makefile";
    
    public OpenMakefileAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        String makefileName = BuildAction.findMakefile();
        if (makefileName != null) {
            Evergreen.getInstance().openFile(makefileName);
        }
    }
}
