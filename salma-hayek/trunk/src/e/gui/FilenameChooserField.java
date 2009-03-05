package e.gui;

import e.forms.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 * Combines a pathname text field with a button that opens a file system browser.
 * This is a common feature in a number of systems, and can be implemented just once.
 * 
 * FIXME: auto-completion in the text field would be great. Apple use a timer and the tab key, but we might have other fields in the same dialog.
 */
public class FilenameChooserField extends JPanel {
    // FIXME: we could use this to check whether the current pathname is suitable, and maybe turn the text red if not.
    // FIXME: we would probably also need to know whether the field was for existing files/directories or new ones (and disable the check in the latter case).
    private final int fileChooserMode;
    
    /**
     * We create a field to hold the user's chosen pathname.
     * Changes to this cause real-time updates to the companion field.
     */
    private final JTextField pathnameField = new JTextField("", 40);
    
    // We're (optionally) given this one, if we're supposed to update another field with the chosen leafname.
    // FIXME: own this ourselves, and offer addActionListener or something to be informed of changes?
    // FIXME: move the whole "companion" thing out into Evergreen, on the assumption that there will never be another user?
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
        pathnameField.setDocument(new FilenameDocument());
        pathnameField.getDocument().addDocumentListener(new DocumentAdapter() {
            public void documentChanged() {
                updateCompanionNameField();
            }
        });
        
        // Ensure the text field will take up any spare space.
        final Dimension maxSize = pathnameField.getPreferredSize();
        maxSize.width = Integer.MAX_VALUE;
        pathnameField.setMaximumSize(maxSize);
        
        setLayout(new BorderLayout(GuiUtilities.getComponentSpacing(), 0));
        add(pathnameField, BorderLayout.CENTER);
        add(makeButton(), BorderLayout.EAST);
    }
    
    private JButton makeButton() {
        final JButton button = new JButton(new BrowseAction());
        button.setFocusable(false); // Tabbing over this button in Evergreen's "Add Workspace" dialog is annoying.
        return button;
    }
    
    private class BrowseAction extends AbstractAction {
        public BrowseAction() {
            GuiUtilities.configureAction(this, GuiUtilities.isMacOs() ? "Choose..." : "_Browse...", null);
        }
        
        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(fileChooserMode);
            chooser.setCurrentDirectory(FileUtilities.fileFromString(pathnameField.getText()));
            
            int result = chooser.showDialog(null, "Choose");
            if (result == JFileChooser.APPROVE_OPTION) {
                pathnameField.setText(FileUtilities.getUserFriendlyName(chooser.getSelectedFile()));
            }
        }
    }
    
    private void updateCompanionNameField() {
        if (companionNameField == null) {
            return; // A name field is optional, so we might not have one to play with.
        }
        
        final File file = FileUtilities.fileFromString(pathnameField.getText());
        companionNameField.setText(file.getName());
    }
    
    // Protect Windows users against accidental use of '/', which may work, but is likely to lead to confusion.
    // Likewise, protect Unix users who might accidentally use the Windows separator, unlikely though that is.
    private static class FilenameDocument extends PlainDocument {
        @Override public void insertString(int offset, String s, AttributeSet attributes) throws BadLocationException {
            if (s != null) {
                s = s.replace('/', File.separatorChar).replace('\\', File.separatorChar);
            }
            super.insertString(offset, s, attributes);
        }
    }
}
