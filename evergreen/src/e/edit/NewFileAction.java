package e.edit;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

/**
 * An action for the operation of creating and opening a new file.
 */
public class NewFileAction extends AbstractAction {
    private static final FilenameChooserField filenameField = new FilenameChooserField(JFileChooser.FILES_AND_DIRECTORIES);
    
    public NewFileAction() {
        GuiUtilities.configureAction(this, "_New File...", GuiUtilities.makeKeyStroke("N", false));
        GnomeStockIcon.useStockIcon(this, "gtk-new");
    }
    
    @Override public boolean isEnabled() {
        return !Evergreen.getInstance().getWorkspaces().isEmpty();
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = ETextAction.getFocusedTextWindow();
        if (window != null) {
            // Fill in the full path.
            final String filename = window.getFilename();
            filenameField.setPathname(filename);
            // Select only the basename (without extension).
            // The user is highly likely to want to reuse the directory.
            // The user is pretty likely to want to create another file of the same type.
            // Selecting the basename helps in both cases, and at least puts the caret in a useful place if they actually want to create "file.cpp" to match "file.h".
            int basenameLength = filename.indexOf('.');
            if (basenameLength == -1) {
                basenameLength = filename.length();
            }
            filenameField.getPathnameField().select(window.getContext().length(), basenameLength);
        } else {
            filenameField.setPathname(Evergreen.getInstance().getCurrentWorkspace().getRootDirectory());
        }

        do {
            FormBuilder form = new FormBuilder(Evergreen.getInstance().getFrame(), "New File");
            form.getFormPanel().addRow("Filename:", filenameField);
            boolean okay = form.show("Create");
            
            if (okay == false) {
                return;
            }
        } while (createNewFile(filenameField.getPathname()) == false);
    }

    private boolean createNewFile(String filename) {
        Evergreen editor = Evergreen.getInstance();
        Path newFile = FileUtilities.pathFrom(filename);
        try {
            if (newFile.isAbsolute() == false) {
                // A new file, if no absolute path is specified, should be
                // created relative to the root of the current workspace.
                newFile = FileUtilities.pathFrom(editor.getCurrentWorkspace().getRootDirectory(), filename);
            }
            Path directory = newFile.getParent();
            if (Files.exists(directory) == false) {
                boolean createDirectory = editor.askQuestion("Create directory?", "The directory \"" + directory + "\" doesn't exist. We can either create the directory for you, or you can go back and re-type the filename.", "Create");
                if (createDirectory == false) {
                    return false;
                }
                Files.createDirectories(directory);
            }
            if (Files.exists(newFile)) {
                editor.showAlert("Couldn't create new file", "File \"" + newFile + "\" already exists.");
            } else {
                createWithInitialContents(newFile);
            }
            editor.openFile(newFile.toString());
            return true;
        } catch (IOException ex) {
            editor.showAlert("Couldn't create new file", "Failed to create file \"" + newFile + "\": " + ex.getMessage() + ".");
            return false;
        }
    }
    
    private void createWithInitialContents(Path file) throws IOException {
        // FIXME: we could use FileType.guessFileTypeByFilename to allow the user to specify scripts that override a single type. YAGNI.
        String defaultBoilerplateGenerator = Evergreen.getResourceFilename("lib", "scripts", "evergreen-boilerplate-generator");
        String boilerplateGenerator = Parameters.getString("boilerplateGenerator", defaultBoilerplateGenerator);
        
        ArrayList<String> lines = new ArrayList<>();
        ProcessUtilities.backQuote(null, new String[] { boilerplateGenerator, file.toString() }, lines, lines);
        Files.write(file, lines, StandardCharsets.UTF_8);
    }
}
