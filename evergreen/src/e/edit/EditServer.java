package e.edit;

import e.util.*;
import java.awt.EventQueue;
import java.io.*;
import java.util.regex.*;

public final class EditServer {
    private Evergreen editor;
    
    public EditServer(Evergreen editor) {
        this.editor = editor;
    }
    
    public void newWorkspace(PrintWriter out, String line) throws Exception {
        Matcher m = Pattern.compile("^newWorkspace (.+)\t(.+)$").matcher(line);
        if (!m.matches()) {
            out.println("Syntax error.");
            return;
        }
        final String workspaceRoot = m.group(1);
        final String workspaceName = m.group(2);
        
        final WorkspaceProperties workspaceProperties = new WorkspaceProperties();
        workspaceProperties.name = workspaceName;
        workspaceProperties.rootDirectory = workspaceRoot;
        
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                editor.createWorkspace(workspaceProperties);
            }
        });
    }
    
    public void closeWorkspace(PrintWriter out, String line) throws Exception {
        final String workspaceName = line.substring("closeWorkspace ".length());
        final Workspace workspace = editor.findWorkspaceByName(workspaceName);
        if (workspace == null) {
            out.println("There is no workspace called \"" + workspaceName + "\".");
            return;
        }
        
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                editor.closeWorkspace(workspace);
            }
        });
    }
    
    public void open(PrintWriter out, String line) {
        String filename = line.substring("open ".length());
        handleOpen(filename, false, out);
    }
    
    public void openAndBlock(PrintWriter out, String line) {
        String filename = line.substring("openAndBlock ".length());
        handleOpen(filename, true, out);
    }
    
    public void rememberState() {
        editor.rememberState();
    }
    
    public void saveAll() {
        SaveAllAction.saveAll(false);
    }
    
    private class Opener implements Runnable {
        private String filename;
        private PrintWriter err;
        private EWindow window;
        
        public Opener(final String filename, final PrintWriter err) {
            this.filename = filename;
            this.err = err;
        }
        
        public EWindow open() {
            try {
                EventQueue.invokeAndWait(this);
            } catch (Exception ex) {
                Log.warn("an unexpected checked exception was thrown", ex);
            }
            return window;
        }
        
        public void run() {
            try {
                this.window = editor.openFileNonInteractively(filename);
                if (GuiUtilities.isMacOs()) {
                    ProcessUtilities.spawn(null, FileUtilities.findSupportBinary("BringProcessToFront").toString());
                } else {
                    editor.getFrame().toFront();
                }
            } catch (Exception ex) {
                err.println(ex.getMessage());
                Log.warn("failed to open " + filename, ex);
            }
        }
    }
    
    private void handleOpen(final String filename, final boolean shouldBlock, final PrintWriter out) {
        // We only have one pipe.
        PrintWriter err = out;
        Opener opener = new Opener(filename, err);
        final EWindow window = opener.open();
        if (shouldBlock == false) {
            return;
        }
        out.println("Waiting for \"" + filename + "\" to be closed...");
        GuiUtilities.waitForWindowToDisappear(window);
    }
}
