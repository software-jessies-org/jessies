package e.edit;

import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import e.forms.*;
import e.gui.*;
import e.util.*;

/**
An action for the operation of creating and opening a new file.
*/
public class NewFileAction extends ETextAction {
    public static final String ACTION_NAME = "New File...";
    
    private static FilenameChooserField filenameField = new FilenameChooserField(JFileChooser.FILES_AND_DIRECTORIES);
    
    public NewFileAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        if (window != null) {
            filenameField.setPathname(window.getContext());
        }

        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Filename:", filenameField);
        boolean okay = FormDialog.show(Edit.getFrame(), "New File", formPanel);
        
        if (okay == false) {
            return;
        }
        
        createNewFile(filenameField.getPathname());
    }

    public void createNewFile(String filename) {
        try {
            File newFile = FileUtilities.fileFromString(filename);
            boolean created = newFile.createNewFile();
            if (created) {
                Edit.getCurrentWorkspace().updateFileList();
                fillWithInitialContents(newFile);
            } else {
                Edit.showAlert("Create", "File '" + filename + "' already exists.");
            }
            Edit.openFile(filename);
        } catch (IOException ex ) {
            ex.printStackTrace();
            Edit.showAlert("Create", "Failed to create file '" + filename + "' (" + ex.getMessage() + ").");
        }
    }
    
    public void fillWithInitialContents(File file) {
        String name = file.getName();
        if (name.endsWith(".h")) {
            // Turn "SomeClass.h" into "SOME_CLASS_H_included".
            String safeName = name.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase().replace('.', '_');
            String macroName = safeName + "_included";
            
            String content = "#ifndef " + macroName + "\n";
            content += "#define " + macroName + "\n";
            content += "\n";
            content += "\n";
            content += "\n";
            content += "#endif\n";
            String result = StringUtilities.writeFile(file, content);
            if (result != null) {
                Edit.showAlert("Create", "Failed to fill '" + file + "' with initial content (" + result + ").");
            }
        }
    }
}
