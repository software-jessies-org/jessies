package e.edit;

import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
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
    private JLabel status = new JLabel(" ");
    
    private boolean haveSearched;
    private boolean notRescanning;
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
            List fileList = workspace.getListOfFilesMatching(regex);
            for (int i = 0; i < fileList.size(); i++) {
                model.addElement(fileList.get(i));
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
                if (notRescanning && e.getClickCount() == 2) {
                    int index = matchList.locationToIndex(e.getPoint());
                    String filename = (String) matchList.getModel().getElementAt(index);
                    Edit.openFile(workspace.getRootDirectory() + File.separator + filename);
                    
                    // Wrestle focus back from the file we've just opened.
                    JDialog dialog = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, matchList);
                    dialog.toFront();
                }
            }
        });
        matchList.setCellRenderer(new EListCellRenderer());
    }
    
    public OpenQuicklyDialog(Workspace workspace) {
        this.workspace = workspace;
        this.haveSearched = false;
    }

    /**
     * Sets the contents of the text field.
     */
    public void setFilenamePattern(String filenamePattern) {
        filenameField.setText(filenamePattern);
    }
    
    class RescanAction extends AbstractAction implements ChangeListener {
        private JButton source;
        
        public RescanAction() {
            super("Rescan");
        }
        
        /**
         * Hides any previous results and starts the rescan.
         */
        public void actionPerformed(ActionEvent e) {
            source = (JButton) e.getSource();
            source.setEnabled(false);
            notRescanning = false;
            switchToFakeList();
            workspace.updateFileList(this);
        }
        
        /**
         * Responsible for providing some visual feedback that we're rescanning.
         */
        private void switchToFakeList() {
            model = new DefaultListModel();
            model.addElement("... rescan in progress ...");
            matchList.setModel(model);
        }
        
        /**
         * Searches again when the file list has finished being updated.
         */
        public void stateChanged(ChangeEvent e) {
            notRescanning = true;
            showMatches();
            source.setEnabled(true);
            source = null;
        }
    }
    
    public JButton makeRescanButton() {
        return new JButton(new RescanAction());
    }
    
    public void showDialog() {
        if (workspace.isFileListUnsuitableFor("Open Quickly")) {
            return;
        }
        
        initMatchList();
        
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Names Containing:", filenameField);
        formPanel.addRow("Matches:", new JScrollPane(matchList));
        formPanel.addRow("", status);
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
}
