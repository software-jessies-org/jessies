package e.edit;

import java.awt.EventQueue;
import java.awt.event.*;
import java.io.*;

import e.util.*;

public final class EditServer {
    private Evergreen editor;
    
    public EditServer(Evergreen editor) {
        this.editor = editor;
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
                this.window = editor.openFileNonInteractively(filename);
                if (GuiUtilities.isMacOs()) {
                    ProcessUtilities.spawn(null, new String[] { FileUtilities.findOnPath("BringProcessToFront").toString() });
                } else {
                    editor.getFrame().toFront();
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
