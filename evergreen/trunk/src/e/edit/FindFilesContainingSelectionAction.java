package e.edit;

import e.util.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Opens the "Find in Files" dialog with a regular expression to match the current
 * selection entered in the dialog's pattern field.
 */
public class FindFilesContainingSelectionAction extends AbstractAction {
    public FindFilesContainingSelectionAction() {
        GuiUtilities.configureAction(this, "Find in Files...", GuiUtilities.makeKeyStroke("G", true));
    }
    
    @Override public boolean isEnabled() {
        return !Evergreen.getInstance().getWorkspaces().isEmpty();
    }
    
    public void actionPerformed(ActionEvent e) {
        Workspace workspace = Evergreen.getInstance().getCurrentWorkspace();
        
        // Get the selection, stripping trailing newlines.
        // ETextAction.getSelectedText returns the empty string if no text window has focus.
        String selection = ETextAction.getSelectedText();
        selection = selection.replaceAll("\n$", "");
        
        // Only use the selection as a pattern if there are no embedded newlines.
        String pattern = null;
        if (selection.length() > 0 && selection.contains("\n") == false) {
            pattern = StringUtilities.regularExpressionFromLiteral(selection);
        }
        String directory = guessDirectoryToSearchIn();
        String filenamePattern = StringUtilities.regularExpressionFromLiteral(directory);
        workspace.showFindInFilesDialog(pattern, filenamePattern);
    }
    
    public String guessDirectoryToSearchIn() {
        ETextWindow textWindow = ETextAction.getFocusedTextWindow();
        if (textWindow == null) {
            return "";
        }
        
        // If there aren't many files in the workspace, don't bother automatically restricting the search to a specific directory.
        // Note that "" actually means "use whatever's already in the field" rather than "nothing".
        Workspace workspace = Evergreen.getInstance().getCurrentWorkspace();
        int indexedFileCount = workspace.getFileList().getIndexedFileCount();
        if (indexedFileCount != -1 && indexedFileCount < 1000) {
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
