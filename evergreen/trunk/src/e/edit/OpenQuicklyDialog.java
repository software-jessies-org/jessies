package e.edit;

import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import e.forms.*;
import e.gui.*;

/**
 * Improves on Apple Project Builder's "Open Quickly",
 * which just pops up a dialog where you type a name. We have
 * a list -- updated as you type in the filename field -- showing what
 * files match the regular expression you've typed. You can
 * double-click individual entries to open them, or hit Return if
 * there's just the one.
 */
public class OpenQuicklyDialog {
    private FileNamePatternField filenameField = new FileNamePatternField();
    private JList matchList;
    
    private boolean haveSearched;
    private DefaultListModel model;
    private PatternSyntaxException patternSyntaxException;
    
    private Workspace workspace;
    
    public class FileNamePatternField extends EMonitoredTextField {
        public FileNamePatternField() {
            super(40);
        }
        
        /** Tracks (cheaply) *every* keypress, because it's important to know whether the search results are up-to-date. */
        public void textChanged() {
            super.textChanged();
            haveSearched = false;
        }
        
        public void timerExpired() {
            haveSearched = true;
            showMatches();
        }
    }
    
    public synchronized void showMatches() {
        String regex = filenameField.getText();
    
        this.model = new DefaultListModel();
        try {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            List fileList = workspace.getFileList();
            for (int i = 0; i < fileList.size(); i++) {
                String candidate = (String) fileList.get(i);
                Matcher matcher = pattern.matcher(candidate);
                if (matcher.find()) {
                    model.addElement(candidate);
                }
            }
        } catch (PatternSyntaxException ex) {
            patternSyntaxException = ex;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        matchList.setModel(model);
        
        // This means the user can just hit Return if there's only one match.
        if (model.getSize() == 1) {
            matchList.setSelectedIndex(0);
        }
    }
    
    public void initMatchList() {
        if (matchList != null) {
            return;
        }

        matchList = new JList();
        matchList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = matchList.locationToIndex(e.getPoint());
                    String filename = (String) matchList.getModel().getElementAt(index);
                    Edit.openFile(workspace.getRootDirectory() + File.separator + filename);
                    
                    // Wrestle focus back from the file we've just opened.
                    JDialog dialog = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, matchList);
                    dialog.toFront();
                }
            }
        });
    }
    
    public OpenQuicklyDialog(Workspace workspace) {
        this.workspace = workspace;
        this.haveSearched = false;
    }

    /**
     * Sets the contents of the text field.
     * The value null causes the filename pattern to stay as it was.
     */
    public void setFilenamePattern(String filenamePattern) {
        if (filenamePattern == null) {
            return;
        }
        filenameField.setText(filenamePattern);
    }
    
    public JButton makeRescanButton() {
        JButton rescanButton = new JButton("Rescan");
        rescanButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                workspace.updateFileList();
            }
        });
        return rescanButton;
    }
    
    public void showDialog() {
        if (workspace.getFileList() == null) {
            Edit.showAlert("Open Quickly", "The list of files for " + workspace.getTitle() + " is not yet available.");
            return;
        }
        
        initMatchList();
        
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Names Containing:", filenameField);
        formPanel.addRow("Matches:", new JScrollPane(matchList));
        boolean okay = FormDialog.show(Edit.getFrame(), "Open Quickly", formPanel, makeRescanButton());
        
        if (okay == false) {
            return;
        }
        
        // Ensure that, if the user was too fast in hitting return, we've definitely searched.
        if (haveSearched == false) {
            showMatches();
        }
        
        openSelectedFilesFromList();
    }
    
    public void openSelectedFilesFromList() {
        ListModel list = matchList.getModel();
        int[] indices = matchList.getSelectedIndices();
        for (int i = 0; i < indices.length; i++) {
            String filename = (String) list.getElementAt(indices[i]);
            Edit.openFile(workspace.getRootDirectory() + File.separator + filename);
        }
    }
    
    /** Always returns true because there's no reason not to open a file. */
    public boolean isEnabled() {
        return true;
    }
}
