package e.gui;

import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import e.util.*;

/**
 * Combines a pathname text field with a button that opens a file system browser. This is a common
 * feature in a number of systems, and can be implemented just once.
 *
 * FIXME: if the companion field gets updated, should we try to update the pathname field?
 */
public class FilenameChooserField extends JPanel implements ActionListener {
    private int fileChooserMode;
    
    /** We create this one, to hold the user's chosen pathname. */
    private JTextField pathnameField = new JTextField("", 40);
    
    /** We're (optionally) given this one, if we're supposed to update another field with the chosen leafname. */
    private JTextField companionNameField;
    
    /**
     * fileChooserMode is one of the JFileChooser file selection mode constants:
     * 
     * JFileChooser.FILES_ONLY
     * JFileChooser.DIRECTORIES_ONLY
     * JFileChooser.FILES_AND_DIRECTORIES
     */
    public FilenameChooserField(int fileChooserMode) {
        this.fileChooserMode = fileChooserMode;
        initUI();
    }
    
    public String getPathname() {
        return pathnameField.getText();
    }
    
    public void setPathname(String pathname) {
        pathnameField.setText(pathname);
    }
    
    public void setCompanionNameField(JTextField field) {
        companionNameField = field;
    }
    
    private void initUI() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        pathnameField.setMaximumSize(pathnameField.getPreferredSize());
        add(pathnameField);
        add(Box.createHorizontalStrut(10));
        add(makeButton());
    }
    
    private JButton makeButton() {
        String label = GuiUtilities.isMacOs() ? "Choose..." : "Browse...";
        JButton result = new JButton(label);
        result.addActionListener(this);
        return result;
    }
    
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(fileChooserMode);
        // JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(FileUtilities.fileFromString(pathnameField.getText()));
        
        int result = chooser.showDialog(null, "Choose");
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            updateFilenameField(file);
            updateCompanionNameField(file);
        }
    }
    
    private void updateFilenameField(File file) {
        pathnameField.setText(FileUtilities.getUserFriendlyName(file));
    }
    
    private void updateCompanionNameField(File file) {
        if (companionNameField == null) {
            return; // A name field is optional, so we might not have one to play with.
        }
        
        companionNameField.setText(file.getName());
    }
}
