package e.edit;

import java.io.*;
import java.awt.event.*;

import e.util.*;

/**
 * Opens the "Find in Files" dialog with a regular expression to match the current
 * selection entered in the dialog's pattern field.
 */
public class FindFilesContainingSelectionAction extends ETextAction {
    public FindFilesContainingSelectionAction() {
        super("Find in Files...");
        putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStroke("G", true));
    }
    
    public void actionPerformed(ActionEvent e) {
        Workspace workspace = Evergreen.getInstance().getCurrentWorkspace();
        // FindInFilesDialog.showDialog will make this check, but not before
        // guessDirectoryToSearchIn has already failed.
        if (workspace.getFileList().isFileListUnsuitableFor("Find in Files")) {
            return;
        }
        
        String selection = getSelectedText();
        
        // Remove trailing newlines.
        selection = selection.replaceAll("\n$", "");
        
        // Only use the selection as a pattern if there are no embedded newlines.
        String pattern = null;
        if (selection.length() > 0 && selection.contains("\n") == false) {
            pattern = StringUtilities.regularExpressionFromLiteral(selection);
        }
        String directory = guessDirectoryToSearchIn();
        workspace.showFindInFilesDialog(pattern, directory);
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
        Workspace workspace = Evergreen.getInstance().getCurrentWorkspace();
        if (workspace.getFileList().getIndexedFileCount() < 1000) {
            return "";
        }
        
        String directory = textWindow.getContext();
        
        // Strip the workspace root.
        String possiblePrefix = Evergreen.getInstance().getCurrentWorkspace().getRootDirectory();
        if (directory.startsWith(possiblePrefix)) {
            directory = directory.substring(possiblePrefix.length());
            if (directory.startsWith(File.separator)) {
                directory = directory.substring(1);
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
