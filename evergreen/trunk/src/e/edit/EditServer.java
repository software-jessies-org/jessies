package e.edit;

import java.awt.EventQueue;
import java.awt.event.*;
import java.io.*;

import e.util.*;

public final class EditServer extends InAppServer {
    private Edit edit;
    
    public EditServer(Edit edit) {
        super("Edit", edit.getPreferenceFilename("edit-server-port"));
        this.edit = edit;
    }
    
    public boolean handleCommand(String line, PrintWriter out) {
        if (line.startsWith("open-and-block ")) {
            String filename = line.substring("open-and-block ".length());
            handleOpen(filename, true, out);
        } else if (line.startsWith("open ")) {
            String filename = line.substring("open ".length());
            handleOpen(filename, false, out);
        } else if (line.equals("remember-state")) {
            edit.rememberState();
        } else if (line.equals("save-all")) {
            SaveAllAction.saveAll(false);
        } else {
            return false;
        }
        return true;
    }
    
    private class Opener implements Runnable {
        private String filename;
        private boolean shouldBlock;
        private PrintWriter out;
        private EWindow window;
        
        public Opener(final String filename, final PrintWriter out) {
            this.filename = filename;
            this.out = out;
        }
        
        public EWindow open() {
            try {
                EventQueue.invokeAndWait(this);
            } catch (Exception ex) {
                out.println(ex.getMessage());
            }
            return window;
        }
        
        public void run() {
            try {
                this.window = edit.openFileNonInteractively(filename);
                if (GuiUtilities.isMacOs()) {
                    ProcessUtilities.spawn(null, new String[] { FileUtilities.findOnPath("BringProcessToFront").toString() });
                } else {
                    edit.getFrame().toFront();
                }
                out.println("File '" + filename + "' opened OK.");
            } catch (Exception ex) {
                out.println(ex.getMessage());
                ex.printStackTrace(out);
            }
        }
    }
    
    private void handleOpen(final String filename, final boolean shouldBlock, final PrintWriter out) {
        Opener opener = new Opener(filename, out);
        final EWindow window = opener.open();
        if (shouldBlock) {
            final Object lock = new Object();
            // FIXME: is this really the easiest way to watch for the component being removed from the hierarchy?
            window.addHierarchyListener(new HierarchyListener() {
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && window.isShowing() == false) {
                        System.out.println(window + " closed");
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                }
            });
            try {
                synchronized (lock) {
                    lock.wait();
                }
            } catch (Exception ex) {
                out.println(ex.getMessage());
            }
            out.println("Closed " + filename);
        }
    }
}
