package e.edit;

import java.awt.event.*;
import javax.swing.*;

import e.forms.*;
import e.util.*;

public class SetBuildTargetAction extends ETextAction {
    public static final String ACTION_NAME = "Set Build Target...";
    
    private JTextField buildTargetField = new JTextField("", 40);
    
    public SetBuildTargetAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        buildTargetField.setText(Parameters.getParameter("make.target"));
        
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Build Target:", buildTargetField);
        boolean okay = FormDialog.show(Edit.getFrame(), "Build Properties", formPanel);
        
        if (okay == false) {
            return;
        }
        
        System.setProperty("make.target", buildTargetField.getText());
    }
}
