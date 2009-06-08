package e.edit;

import java.awt.event.*;

public class OpenMakefileAction extends ETextAction {
    public OpenMakefileAction() {
        super("Open _Makefile", null);
    }
    
    public void actionPerformed(ActionEvent e) {
        final BuildAction.BuildTool buildTool = BuildAction.chooseBuildTool();
        if (buildTool != null) {
            Evergreen.getInstance().openFile(buildTool.makefileName);
        }
    }
}
