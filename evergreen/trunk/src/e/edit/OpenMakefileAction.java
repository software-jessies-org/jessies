package e.edit;

import java.awt.event.*;

public class OpenMakefileAction extends ETextAction {
    public OpenMakefileAction() {
        super("Open Makefile");
    }
    
    public void actionPerformed(ActionEvent e) {
        String makefileName = BuildAction.findMakefile();
        if (makefileName != null) {
            Evergreen.getInstance().openFile(makefileName);
        }
    }
}
