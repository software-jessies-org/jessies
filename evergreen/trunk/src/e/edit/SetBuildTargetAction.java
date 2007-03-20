package e.edit;

import java.awt.event.*;
import javax.swing.*;
import e.forms.*;

public class SetBuildTargetAction extends ETextAction {
    private JTextField buildTargetField = new JTextField("", 40);
    
    public SetBuildTargetAction() {
        super("Set Build Target...");
    }
    
    public void actionPerformed(ActionEvent e) {
        Workspace workspace = Evergreen.getInstance().getCurrentWorkspace();

        buildTargetField.setText(workspace.getBuildTarget());
        
        FormBuilder form = new FormBuilder(Evergreen.getInstance().getFrame(), workspace.getTitle() + " Build Target");
        form.getFormPanel().addRow("Build Target:", buildTargetField);
        boolean okay = form.show("Apply");
        
        if (okay == false) {
            return;
        }
        
        workspace.setBuildTarget(buildTargetField.getText());
    }
}
