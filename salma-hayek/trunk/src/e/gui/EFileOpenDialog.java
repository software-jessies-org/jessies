package e.gui;

import java.awt.*;
import java.io.*;
import javax.swing.*;
import e.util.*;

/**
 * An open dialog that uses whichever is best out of the AWT
 * and Swing offerings for your platform.
 */
public class EFileOpenDialog {
    private FileDialog awtOpenDialog;
    private JFileChooser swingOpenDialog;
    private int swingResult;
    
    /**
     * Creates a new open dialog starting in the given directory.
     * Note that your program shouldn't keep creating these: it
     * should create one and re-use it, so the state the user
     * left behind (mostly which directory they were in) stays as
     * it was across openings.
     */
    public EFileOpenDialog(Frame parent, String directory) {
        if (useAwt()) {
            awtOpenDialog = new FileDialog(parent, "Open", FileDialog.LOAD);
            awtOpenDialog.setDirectory(directory);
        } else {
            swingOpenDialog = new JFileChooser();
            swingOpenDialog.setCurrentDirectory(FileUtilities.fileFromString(directory));
        }
    }
    
    /**
     * Shows this dialog and waits for the user to make a selection.
     */
    public void show() {
        if (awtOpenDialog != null) {
            awtOpenDialog.show();
        } else {
            swingResult = swingOpenDialog.showDialog(null, "Open");
        }
    }
    
    /**
     * Returns the full pathname of the chosen file, or null if none was chosen.
     */
    public String getFile() {
        if (awtOpenDialog != null) {
            String leaf = awtOpenDialog.getFile();
            if (leaf == null) {
                return null;
            }
            return awtOpenDialog.getDirectory() + File.separator + leaf;
        } else {
            if (swingResult != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            return swingOpenDialog.getSelectedFile().toString();
        }
    }
    
    /**
     * Decides whether we should use AWT or Swing for our open dialog.
     * At the moment, we use the native dialogs on Mac OS and Windows,
     * because they're both great, but we avoid the Motif dialog we're
     * likely to get elsewhere because it's appalling.
     */
    private boolean useAwt() {
        return GuiUtilities.isMacOs() || GuiUtilities.isWindows();
    }
}
