package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import e.forms.*;
import e.gui.*;
import e.util.*;

/**
 * Improves on Apple Project Builder's "Open Quickly",
 * which just pops up a dialog where you type a name. We have
 * a list -- updated as you type in the filename field -- showing what
 * files match the regular expression you've typed. You can
 * double-click individual entries to open them, or hit Return if
 * there's just the one.
 */
public class OpenQuicklyDialog {
    private JTextField filenameField = new JTextField(40);
    private JList matchList;
    private JLabel status = new JLabel(" ");
    
    private DefaultListModel model;
    
    private Workspace workspace;
    
    private void setStatus(boolean good, String text) {
        status.setForeground(good ? Color.BLACK : Color.RED);
        status.setText(text);
    }
    
    public synchronized void showMatches() {
        String regex = filenameField.getText();
    
        this.model = new DefaultListModel();
        try {
            List fileList = workspace.getFileList().getListOfFilesMatching(regex);
            for (int i = 0; i < fileList.size(); i++) {
                model.addElement(fileList.get(i));
            }
            final int totalFileCount = workspace.getFileList().getIndexedFileCount();
            setStatus(true, fileList.size() + " / " + StringUtilities.pluralize(totalFileCount, "file", "files") + " match.");
        } catch (PatternSyntaxException ex) {
            setStatus(false, ex.getDescription());
        }
        matchList.setModel(model);
        
        // If we don't set the selected index, the user won't be able to cycle the focus into the list with the Tab key.
        // This also means the user can just hit Return if there's only one match.
        matchList.setSelectedIndex(0);
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
                    Evergreen.getInstance().openFile(workspace.prependRootDirectory(filename));
                    
                    // Wrestle focus back from the file we've just opened.
                    SwingUtilities.getWindowAncestor(matchList).toFront();
                }
            }
        });
        matchList.setCellRenderer(new EListCellRenderer(true));
    }
    
    public OpenQuicklyDialog(Workspace workspace) {
        this.workspace = workspace;
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
            matchList.setEnabled(false);
            setStatus(true, " ");
            switchToFakeList();
            workspace.getFileList().updateFileList(this);
            filenameField.requestFocusInWindow();
        }
        
        /**
         * Searches again when the file list has finished being updated.
         */
        public void stateChanged(ChangeEvent e) {
            showMatches();
            matchList.setEnabled(true);
            source.setEnabled(true);
            source = null;
        }
    }
    
    /**
     * Responsible for providing some visual feedback that we're rescanning.
     */
    private synchronized void switchToFakeList() {
        model = new DefaultListModel();
        model.addElement("Rescan in progress...");
        matchList.setModel(model);
    }
    
    public JButton makeRescanButton() {
        return new JButton(new RescanAction());
    }
    
    public void showDialog() {
        if (workspace.getFileList().isFileListUnsuitableFor("Open Quickly")) {
            return;
        }
        
        initMatchList();
        
        FormBuilder form = new FormBuilder(Evergreen.getInstance().getFrame(), "Open Quickly");
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Names Containing:", filenameField);
        formPanel.addRow("Matches:", new JScrollPane(matchList));
        formPanel.setStatusBar(status);
        formPanel.setTypingTimeoutActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showMatches();
            }
        });
        form.getFormDialog().setExtraButton(makeRescanButton());
        boolean okay = form.show("Open");
        
        if (okay == false) {
            return;
        }
        
        openSelectedFilesFromList();
    }
    
    public void openSelectedFilesFromList() {
        ListModel list = matchList.getModel();
        for (int index : matchList.getSelectedIndices()) {
            String filename = (String) list.getElementAt(index);
            Evergreen.getInstance().openFile(workspace.prependRootDirectory(filename));
        }
    }
}
