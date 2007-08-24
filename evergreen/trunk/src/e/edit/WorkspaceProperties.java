package e.edit;

import e.forms.*;
import e.gui.*;
import e.util.*;
import javax.swing.*;

/**
 * Factors out code common to the workspace creation/modification dialogs.
 */
public class WorkspaceProperties {
    public String name;
    public String rootDirectory;
    
    public boolean showWorkspacePropertiesDialog(String dialogTitle, String buttonTitle) {
        JTextField nameField = new JTextField("", 40);
        if (name != null) {
            nameField.setText(name);
        }
        
        FilenameChooserField filenameChooserField = new FilenameChooserField(JFileChooser.DIRECTORIES_ONLY);
        filenameChooserField.setCompanionNameField(nameField);
        if (rootDirectory != null) {
            filenameChooserField.setPathname(rootDirectory);
        }
        
        FormBuilder form = new FormBuilder(Evergreen.getInstance().getFrame(), dialogTitle);
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Root Directory:", filenameChooserField);
        formPanel.addRow("Name:", nameField);
        
        while (form.show(buttonTitle)) {
            String message = FileUtilities.checkDirectoryExistence(filenameChooserField.getPathname());
            if (message == null) {
                name = nameField.getText();
                rootDirectory = filenameChooserField.getPathname();
                return true;
            }
            Evergreen.getInstance().showAlert(message, "The pathname you supply must exist, and must be a directory.");
        }
        return false;
    }
}
