package e.edit;

import java.awt.event.*;
import javax.swing.*;
import e.forms.*;
import e.gui.*;
import e.util.*;

/**
 * Opens a dialog where the user can specify a name and root directory for a new workspace,
 * which will then be created.
 */
public class AddWorkspaceAction extends AbstractAction {
    private static final String ACTION_NAME = "Add Workspace...";
    
    public AddWorkspaceAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        JTextField nameField = new JTextField("", 40);
        
        FilenameChooserField filenameChooserField = new FilenameChooserField(JFileChooser.DIRECTORIES_ONLY);
        filenameChooserField.setCompanionNameField(nameField);
        
        FormBuilder form = new FormBuilder(Edit.getInstance().getFrame(), "Add Workspace");
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Root Directory:", filenameChooserField);
        formPanel.addRow("Name:", nameField);
        
        boolean finished = false;
        while (finished == false) {
            boolean okay = form.show("Add");
            if (okay == false) {
                return;
            }
            
            String message = FileUtilities.checkDirectoryExistence(filenameChooserField.getPathname());
            if (message != null) {
                Edit.getInstance().showAlert(message, "The name you supply must exist, and must be a directory.");
            } else {
                Workspace workspace = Edit.getInstance().createWorkspace(nameField.getText(), filenameChooserField.getPathname());
                workspace.updateFileList(null);
                finished = true;
            }
        }
    }
}
