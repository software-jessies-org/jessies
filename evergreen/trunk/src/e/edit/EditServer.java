package e.edit;

import java.io.*;
import javax.swing.*;

import e.util.*;

public final class EditServer extends InAppServer {
    public EditServer() {
        super("Edit", Edit.getPreferenceFilename("edit-server-port"));
    }
    
    public boolean handleCommand(String line, PrintWriter out) {
        if (line.startsWith("open ")) {
            String filename = line.substring("open ".length());
            handleOpen(filename, out);
        } else if (line.equals("remember-state")) {
            Edit.getInstance().rememberState();
        } else if (line.equals("save-all")) {
            SaveAllAction.saveAll(false);
        } else {
            return false;
        }
        return true;
    }
    
    private void handleOpen(final String filename, final PrintWriter out) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        Edit.openFileNonInteractively(filename);
                        Edit.getFrame().toFront();
                        out.println("File '" + filename + "' opened OK.");
                    } catch (Exception ex) {
                        out.println(ex.getMessage());
                    }
                }
            });
        } catch (Exception ex) {
            out.println(ex.getMessage());
        }
    }
}
