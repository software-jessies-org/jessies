package e.edit;

import java.awt.event.*;
import javax.swing.*;
import e.forms.*;

public class SetBuildTargetAction extends ETextAction {
    public static final String ACTION_NAME = "Set Build Target...";
    
    private JTextField buildTargetField = new JTextField("", 40);
    
    public SetBuildTargetAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        Workspace workspace = Edit.getCurrentWorkspace();

        buildTargetField.setText(workspace.getBuildTarget());
        
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Build Target:", buildTargetField);
        boolean okay = FormDialog.show(Edit.getFrame(), workspace.getTitle() + " Build Target", formPanel, "Apply");
        
        if (okay == false) {
            return;
        }
        
        workspace.setBuildTarget(buildTargetField.getText());
    }
}
