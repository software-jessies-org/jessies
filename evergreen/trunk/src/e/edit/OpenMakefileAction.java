package e.edit;

import java.awt.event.*;

public class OpenMakefileAction extends ETextAction {
    public static final String ACTION_NAME = "Open Makefile";
    
    public OpenMakefileAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        if (window == null) {
            return;
        }
        String makefileName = BuildAction.findMakefile(window.getContext());
        if (makefileName != null) {
            Edit.getInstance().openFile(makefileName);
        }
    }
}
