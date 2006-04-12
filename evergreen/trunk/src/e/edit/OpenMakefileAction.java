package e.edit;

import java.awt.event.*;

public class OpenMakefileAction extends ETextAction {
    public static final String ACTION_NAME = "Open Makefile";
    
    public OpenMakefileAction() {
        super(ACTION_NAME);
    }
    
    private String getSearchDirectory() {
        ETextWindow window = getFocusedTextWindow();
        if (window != null) {
            return window.getContext();
        } else {
            Workspace workspace = Edit.getInstance().getCurrentWorkspace();
            return (workspace == null) ? null : workspace.getRootDirectory();
        }
    }
    
    public void actionPerformed(ActionEvent e) {
        String searchDirectory = getSearchDirectory();
        if (searchDirectory != null) {
            String makefileName = BuildAction.findMakefile(searchDirectory);
            if (makefileName != null) {
                Edit.getInstance().openFile(makefileName);
            }
        }
    }
}
