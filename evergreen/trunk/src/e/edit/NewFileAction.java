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
        putValue(ACCELERATOR_KEY, EditMenuBar.makeKeyStroke("N", false));
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        if (window != null) {
            filenameField.setPathname(window.getContext());
        }

        do {
            FormPanel formPanel = new FormPanel();
            formPanel.addRow("Filename:", filenameField);
            boolean okay = FormDialog.show(Edit.getFrame(), "New File", formPanel, "Create");
            
            if (okay == false) {
                return;
            }
        } while (createNewFile(filenameField.getPathname()) == false);
    }

    public boolean createNewFile(String filename) {
        try {
            File newFile = FileUtilities.fileFromString(filename);
            File directory = newFile.getParentFile();
            if (directory.exists() == false) {
                boolean createDirectory = Edit.askQuestion("New File", "The directory '" + directory + "' doesn't exist. Edit can either create the directory for you, or you can go back and re-type the filename.", "Create");
                if (createDirectory == false) {
                    return false;
                }
                directory.mkdirs();
            }
            boolean created = newFile.createNewFile();
            if (created) {
                Edit.getCurrentWorkspace().updateFileList(null);
                fillWithInitialContents(newFile);
            } else {
                Edit.showAlert("Create", "File '" + filename + "' already exists.");
            }
            Edit.openFile(filename);
            return true;
        } catch (IOException ex ) {
            ex.printStackTrace();
            Edit.showAlert("Create", "Failed to create file '" + filename + "' (" + ex.getMessage() + ").");
            return false;
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
