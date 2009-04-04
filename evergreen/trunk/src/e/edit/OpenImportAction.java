package e.edit;

import e.ptextarea.*;
import e.util.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * Opens the file corresponding to the selected import.
 * 
 * For C++, this would be the header file corresponding to the selected #include.
 * For Java, this would be the source file corresponding to the selected import.
 */
public class OpenImportAction extends ETextAction {
    private static final HashMap<FileType, String> importPaths = initImportPaths();
    
    public OpenImportAction() {
        super("Open _Import...", GuiUtilities.makeKeyStroke("O", true));
    }
    
    public void actionPerformed(ActionEvent e) {
        final Evergreen editor = Evergreen.getInstance();
        final ETextWindow textWindow = getFocusedTextWindow();
        if (textWindow == null) {
            return;
        }
        
        String path = getSelectedText().trim();
        if (path.length() == 0) {
            // There was no selection, so grab the whole of the current line.
            // FIXME: if we did this, and still find nothing, how about opening a dialog? sometimes it's easier to ask for a #include by name than to find an existing copy of the name (or add one somewhere).
            final ETextArea textArea = textWindow.getTextArea();
            path = textArea.getLineText(textArea.getLineOfOffset(textArea.getSelectionStart()));
        }
        
        // Rewrite boilerplate to get a path fragment.
        final FileType fileType = textWindow.getFileType();
        if (fileType == FileType.C_PLUS_PLUS) {
            // Get rid of "#include <...>".
            path = path.replaceAll("^\\s*#\\s*include\\s+[<\"]([^>\"]+)[\">].*", "$1");
        } else if (fileType == FileType.JAVA) {
            // Get rid of "import ...;".
            path = path.replaceAll("^\\s*import\\s+(.+)\\s*;.*", "$1");
            // Rewrite '.' as File.separatorChar, and append the missing ".java".
            path = path.replace('.', '/') + ".java";
        } else if (fileType == FileType.RUBY) {
            // Get rid of "require '...'".
            path = path.replaceAll("^\\s*require\\s+['\"](.+)['\"].*", "$1");
            // Make sure the trailing ".rb" is present.
            if (path.endsWith(".rb") == false) {
                path += ".rb";
            }
        }
        
        if (path.startsWith("~") || path.startsWith("/")) {
            // If we have an absolute name, we can go straight there.
            // The user probably meant to use "Open Quickly", but there's no need to punish them.
            editor.openFile(path);
            return;
        }
        
        // FIXME: we should probably allow "." and interpret it as "the directory containing the current file". maybe just implicitly always check there first?
        List<String> importPath = importPathForFileType(fileType);
        for (String importDir : importPath) {
            File file;
            if (importDir.startsWith("/") || importDir.startsWith("~")) {
                file = FileUtilities.fileFromParentAndString(importDir, path);
            } else {
                // We interpret non-absolute paths as being rooted at the current workspace's root.
                final String root = editor.getCurrentWorkspace().getRootDirectory();
                file = new File(FileUtilities.fileFromParentAndString(root, importDir), path);
            }
            
            if (file.exists()) {
                // Even if there would be multiple matches, we just take the first one we come across.
                editor.openFile(file.toString());
                return;
            }
        }
        editor.showAlert("Couldn't open imported file", "There was no file \"" + path + "\" under any of the directories on the import path:\n" + StringUtilities.join(importPath, ":"));
    }
    
    private static HashMap<FileType, String> initImportPaths() {
        HashMap<FileType, String> result = new HashMap<FileType, String>();
        
        // Set up the defaults.
        result.put(FileType.C_PLUS_PLUS, "/usr/include/:/usr/include/c++/4.2/:native/Headers/");
        // FIXME: backquote ruby -e 'puts($:.join(":"))'
        result.put(FileType.RUBY, "/usr/lib/ruby/1.8/");
        
        // Override or supplement those with any user-configured import paths.
        for (String fileTypeName : FileType.getAllFileTypeNames()) {
            // FIXME: when we have per-FileType configuration, get the import path from there.
            String importPath = Parameters.getString(fileTypeName + ".importPath", null);
            if (importPath != null) {
                result.put(FileType.fromName(fileTypeName), importPath.trim());
            }
        }
        
        return result;
    }
    
    private static List<String> importPathForFileType(FileType fileType) {
        if (importPaths.containsKey(fileType)) {
            return Arrays.asList(importPaths.get(fileType).split(":"));
        } else {
            return Arrays.asList(new String[0]);
        }
    }
}
