package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.event.*;

/**
 * Opens the file corresponding to the selected import.
 * 
 * For C++, this would be the header file corresponding to the selected #include.
 * For Java, this would be the source file corresponding to the selected import.
 */
public class OpenImportAction extends ETextAction {
    public OpenImportAction() {
        super("Open Import...");
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("O", true));
    }
    
    public void actionPerformed(ActionEvent e) {
        // FIXME: if there's no selection, take the whole of the current line.
        String path = getSelectedText().trim();
        // FIXME: chop off boilerplate like "#include <...>" or "import ...;", and rewrite '.' in Java (but not C++!) as File.separatorChar.
        if (path.startsWith("~") || path.startsWith("/")) {
            // If we have an absolute name, we can go straight there.
            // The user probably meant to use "Open Quickly", but there's no need to punish them.
            Evergreen.getInstance().openFile(path);
        } else {
            // FIXME: use per-language import paths. This temporarily hard-coded one obviously only applies to C++.
            // FIXME: allow these to be overridden.
            // FIXME: allow path elements that can refer to EDIT_WORKSPACE_ROOT, as in something like "$(EDIT_WORKSPACE_ROOT)/../READONLY/google3/".
            String[] importPath = { "/usr/include/", "/usr/include/c++/4.2/" };
            // FIXME: what if there are multiple matches? does that ever happen in real life?
            for (String importDir : importPath) {
                if (FileUtilities.exists(importDir, path)) {
                    Evergreen.getInstance().openFile(importDir + java.io.File.separator + path);
                    return;
                }
            }
            // FIXME: show the path.
            Evergreen.getInstance().showAlert("Couldn't open imported file", "There was no file \"" + path + "\" under any of the directories on the import path.");
        }
    }
}
