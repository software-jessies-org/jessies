package e.edit;

import java.io.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 * Opens the "Find Files" dialog with the current selection entered in the dialog's
 * pattern field.
 */
public class FindFilesContainingSelectionAction extends SelectedTextAction {
    public FindFilesContainingSelectionAction() {
        super("Find in Files...");
    }
    
    public void actOnSelection(JTextComponent component, String selection) {
        String directory = guessDirectoryToSearchIn(component);
        Edit.getCurrentWorkspace().showFindFilesDialog(selection, directory);
    }
    
    public String guessDirectoryToSearchIn(JTextComponent component) {
        ETextWindow textWindow = (ETextWindow) SwingUtilities.getAncestorOfClass(ETextWindow.class, component);
        if (textWindow == null) {
            return null;
        }
        
        String directory = textWindow.getContext();
        System.err.println(directory);
        
        // Strip the workspace root.
        String possiblePrefix = Edit.getCurrentWorkspace().getRootDirectory();
        if (directory.startsWith(possiblePrefix)) {
            directory = directory.substring(possiblePrefix.length());
            System.err.println(directory);
            if (directory.startsWith(File.separator)) {
                directory = directory.substring(1);
                System.err.println(directory);
            }
        }
        
        // Ensure we have a trailing separator.
        if (directory.endsWith(File.separator) == false) {
            directory += File.separator;
        }
        
        return directory;
    }
}
