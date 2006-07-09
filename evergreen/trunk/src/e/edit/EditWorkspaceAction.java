package e.edit;

import java.awt.event.*;
import javax.swing.*;
import e.forms.*;
import e.gui.*;
import e.util.*;

public class EditWorkspaceAction extends AbstractAction {
    public static final String ACTION_NAME = "Edit Workspace...";
    
    private Workspace boundWorkspace;
    
    // Edit the given workspace.
    public EditWorkspaceAction(Workspace workspace) {
        super(ACTION_NAME);
        this.boundWorkspace = workspace;
    }
    
    // Edit the current workspace at the time the action is performed.
    public EditWorkspaceAction() {
        this(null);
    }
    
    public void actionPerformed(ActionEvent e) {
        Workspace workspace = (boundWorkspace != null) ? boundWorkspace : Evergreen.getInstance().getCurrentWorkspace();
        
        JTextField nameField = new JTextField("", 40);
        nameField.setText(workspace.getTitle());
        
        FilenameChooserField filenameChooserField = new FilenameChooserField(JFileChooser.DIRECTORIES_ONLY);
        filenameChooserField.setCompanionNameField(nameField);
        filenameChooserField.setPathname(workspace.getRootDirectory());
        
        FormBuilder form = new FormBuilder(Evergreen.getInstance().getFrame(), "Workspace Properties");
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Root Directory:", filenameChooserField);
        formPanel.addRow("Name:", nameField);
        
        while (form.show("Apply")) {
            String message = FileUtilities.checkDirectoryExistence(filenameChooserField.getPathname());
            if (message != null) {
                Evergreen.getInstance().showAlert(message, "The pathname you supply must exist, and must be a directory.");
            } else {
                workspace.setTitle(nameField.getText());
                workspace.setRootDirectory(filenameChooserField.getPathname());
                workspace.updateFileList(null);
                return;
            }
        }
    }
}
