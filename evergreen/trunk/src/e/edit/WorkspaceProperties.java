package e.edit;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.io.*;
import javax.swing.*;

/**
 * Factors out code common to the workspace creation/modification dialogs.
 */
public class WorkspaceProperties {
    public String name;
    public String rootDirectory;
    public String buildTarget;
    
    public boolean showWorkspacePropertiesDialog(String dialogTitle, String buttonTitle) {
        JTextField nameField = new JTextField("", 40);
        
        FilenameChooserField filenameChooserField = new FilenameChooserField(JFileChooser.DIRECTORIES_ONLY);
        // If the caller didn't supply a name, setPathname will set a useful default.
        filenameChooserField.setCompanionNameField(nameField);
        if (rootDirectory != null) {
            filenameChooserField.setPathname(rootDirectory);
        }
        // If the caller supplied a name, that should trump setPathname.
        if (name != null) {
            nameField.setText(name);
        }
        
        JTextField buildTargetField = new JTextField("", 40);
        if (buildTarget != null) {
            buildTargetField.setText(buildTarget);
        }
        
        FormBuilder form = new FormBuilder(Evergreen.getInstance().getFrame(), dialogTitle);
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Root Directory:", filenameChooserField);
        formPanel.addRow("Name:", nameField);
        formPanel.addRow("Build Target:", buildTargetField);
        
        while (form.show(buttonTitle)) {
            final String pathname = filenameChooserField.getPathname();
            if (checkPathnameIsDirectory(pathname)) {
                name = nameField.getText();
                if (name.trim().length() == 0) {
                    // Default the visible workspace name to the leafname of the workspace root.
                    name = FileUtilities.fileFromString(pathname).getName();
                }
                rootDirectory = pathname;
                buildTarget = buildTargetField.getText();
                return true;
            }
        }
        return false;
    }
    
    private boolean checkPathnameIsDirectory(String pathname) {
        File proposedDirectory = FileUtilities.fileFromString(pathname);
        if (proposedDirectory.exists() == false) {
            boolean createDirectory = Evergreen.getInstance().askQuestion("Create directory?", "The directory \"" + proposedDirectory + "\" doesn't exist. We can either create the directory for you, or you can go back and re-type the pathname.", "Create");
            if (createDirectory == false) {
                return false;
            }
            proposedDirectory.mkdirs();
            return true;
        } else if (proposedDirectory.isDirectory() == false) {
            Evergreen.getInstance().showAlert("Not a directory", "The path \"" + pathname + "\" exists but does not refer to a directory.");
            return false;
        }
        return true;
    }
}
