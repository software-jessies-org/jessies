package e.edit;

import java.io.*;
import java.awt.event.*;

/**
 * Opens the "Find Files" dialog with the current selection entered in the dialog's
 * pattern field.
 */
public class FindFilesContainingSelectionAction extends ETextAction {
    public FindFilesContainingSelectionAction() {
        super("Find in Files...");
    }
    
    public void actionPerformed(ActionEvent e) {
        String directory = guessDirectoryToSearchIn();
        Edit.getCurrentWorkspace().showFindFilesDialog(getSelectedText(), directory);
    }
    
    public String guessDirectoryToSearchIn() {
        ETextWindow textWindow = getFocusedTextWindow();
        if (textWindow == null) {
            return "";
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
        
        // Ensure we have a trailing separator, unless that would mean that
        // we have a leading separator.
        if (directory.length() > 0 && directory.endsWith(File.separator) == false) {
            directory += File.separator;
        }
        
        return directory;
    }
}
