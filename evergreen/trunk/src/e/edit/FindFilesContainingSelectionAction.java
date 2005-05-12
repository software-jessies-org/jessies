package e.edit;

import java.io.*;
import java.awt.event.*;

import e.util.*;

/**
 * Opens the "Find Files" dialog with a regular expression to match the current
 * selection entered in the dialog's pattern field.
 */
public class FindFilesContainingSelectionAction extends ETextAction {
    public FindFilesContainingSelectionAction() {
        super("Find in Files...");
        putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStroke("G", true));
    }
    
    public void actionPerformed(ActionEvent e) {
        String selection = getSelectedText();
        
        // Remove trailing newlines.
        selection = selection.replaceAll("\n$", "");
        
        // Only use the selection as a pattern if there are no embedded newlines.
        String pattern = null;
        if (selection.length() > 0 && selection.indexOf("\n") == -1) {
            pattern = StringUtilities.regularExpressionFromLiteral(selection);
        }
        String directory = guessDirectoryToSearchIn();
        Edit.getCurrentWorkspace().showFindFilesDialog(pattern, directory);
    }
    
    public String guessDirectoryToSearchIn() {
        ETextWindow textWindow = getFocusedTextWindow();
        if (textWindow == null) {
            return "";
        }
        
        // If there aren't many files in the workspace, don't bother
        // automatically restricting the search to a specific directory.
        // Note that "" actually means "use whatever's already in the
        // field" rather than "nothing".
        Workspace workspace = Edit.getCurrentWorkspace();
        if (workspace.getIndexedFileCount() < 1000) {
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
