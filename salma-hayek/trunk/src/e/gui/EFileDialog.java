package e.gui;

import java.awt.*;
import java.io.*;
import javax.swing.*;
import e.util.*;

/**
 * An open/save dialog that uses whichever is best out of the AWT
 * and Swing offerings for your platform.
 */
public class EFileDialog {
    private FileDialog awtFileDialog;
    private JFileChooser swingFileDialog;
    private String title;
    private Frame parent;
    private int swingResult;
    
    /**
     * Creates a new open dialog starting in the given directory.
     * Note that your program shouldn't keep creating these: it
     * should create one and re-use it, so the state the user
     * left behind (mostly which directory they were in) stays as
     * it was across openings.
     */
    public static EFileDialog makeOpenDialog(Frame parent, String directory) {
        return new EFileDialog(parent, directory, true);
    }
    
    /**
     * Creates a new save dialog starting in the given directory. See also
     * makeOpenDialog.
     */
    public static EFileDialog makeSaveDialog(Frame parent, String directory) {
        return new EFileDialog(parent, directory, false);
    }
    
    private EFileDialog(Frame parent, String directory, boolean isOpen) {
        this.title = isOpen ? "Open" : "Save";
        this.parent = parent;
        if (useAwt()) {
            int kind = isOpen ? FileDialog.LOAD : FileDialog.SAVE;
            awtFileDialog = new FileDialog(parent, title, kind);
            awtFileDialog.setDirectory(directory);
        } else {
            int kind = isOpen ? JFileChooser.OPEN_DIALOG : JFileChooser.SAVE_DIALOG;
            swingFileDialog = new JFileChooser();
            swingFileDialog.setCurrentDirectory(FileUtilities.fileFromString(directory));
            swingFileDialog.setDialogType(kind);
        }
    }
    
    /**
     * Shows this dialog and waits for the user to make a selection.
     */
    public void show() {
        if (awtFileDialog != null) {
            awtFileDialog.setVisible(true);
        } else {
            swingResult = swingFileDialog.showDialog(parent, title);
        }
    }
    
    /**
     * Returns the full pathname of the chosen file, or null if none was chosen.
     */
    public String getFile() {
        if (awtFileDialog != null) {
            String leaf = awtFileDialog.getFile();
            if (leaf == null) {
                return null;
            }
            return awtFileDialog.getDirectory() + File.separator + leaf;
        } else {
            if (swingResult != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            return swingFileDialog.getSelectedFile().toString();
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
