package e.edit;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import org.jdesktop.swingworker.SwingWorker;

/**
 * Improves on Apple Project Builder's "Open Quickly",
 * which just pops up a dialog where you type a name. We have
 * a list -- updated as you type in the filename field -- showing what
 * files match the regular expression you've typed. You can
 * double-click individual entries to open them, or hit Return if
 * there's just the one.
 */
public class OpenQuicklyDialog implements WorkspaceFileList.Listener {
    private JTextField filenameField = new JTextField(40);
    private JList matchList;
    private JLabel status = new JLabel(" ");
    private JButton rescanButton;
    
    /** Which workspace is this "Open Quickly" for? */
    private Workspace workspace;
    
    /** Holds all the UI. The actual "dialog" is in here! */
    private FormBuilder form;
    
    private void setStatus(boolean good, String text) {
        status.setForeground(good ? Color.BLACK : Color.RED);
        status.setText(text);
    }
    
    private class MatchFinder extends SwingWorker<Object, Object> {
        private String regularExpression;
        private DefaultListModel model;
        private boolean statusGood;
        private String statusText;
        
        private MatchFinder(String regularExpression) {
            this.regularExpression = regularExpression;
        }
        
        @Override
        protected Object doInBackground() {
            model = new DefaultListModel();
            statusGood = true;
            try {
                long startTimeMs = System.currentTimeMillis();
                
                List<String> fileList = workspace.getFileList().getListOfFilesMatching(regularExpression);
                for (int i = 0; i < fileList.size(); i++) {
                    model.addElement(fileList.get(i));
                }
                final int indexedFileCount = workspace.getFileList().getIndexedFileCount();
                if (indexedFileCount != -1) {
                    statusText = fileList.size() + " / " + StringUtilities.pluralize(indexedFileCount, "file", "files") + " match.";
                }
                
                long endTimeMs = System.currentTimeMillis();
                Log.warn("Search for files matching \"" + regularExpression + "\" took " + (endTimeMs - startTimeMs) + " ms.");
            } catch (PatternSyntaxException ex) {
                statusGood = false;
                statusText = ex.getDescription();
            }
            return null;
        }
        
        @Override
        public void done() {
            setStatus(statusGood, statusText);
            matchList.setModel(model);
            // If we don't set the selected index, the user won't be able to cycle the focus into the list with the Tab key.
            // This also means the user can just hit Return if there's only one match.
            matchList.setSelectedIndex(0);
        }
    }
    
    public synchronized void showMatches() {
        new MatchFinder(filenameField.getText()).execute();
    }
    
    private void openFileAtIndex(int index) {
        String filename = (String) matchList.getModel().getElementAt(index);
        Evergreen.getInstance().openFile(workspace.prependRootDirectory(filename));
        
        // Now we've opened a new file, that's where focus should go when we're dismissed.
        form.getFormDialog().setShouldRestoreFocus(false);
        
        // Wrestle focus back from the file we've just opened.
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                SwingUtilities.getWindowAncestor(matchList).toFront();
                filenameField.requestFocus();
            }
        });
    }
    
    public void initMatchList() {
        matchList = new JList();
        matchList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = matchList.locationToIndex(e.getPoint());
                    openFileAtIndex(index);
                }
            }
        });
        matchList.setCellRenderer(new EListCellRenderer(true));
        matchList.setFont(ChangeFontAction.getConfiguredFont());
        ComponentUtilities.divertPageScrollingFromTo(filenameField, matchList);
    }
    
    public OpenQuicklyDialog(Workspace workspace) {
        this.workspace = workspace;
        this.rescanButton = RescanWorkspaceAction.makeRescanButton(workspace);
        
        initMatchList();
        initForm();
        
        workspace.getFileList().addFileListListener(this);
    }
    
    private void initForm() {
        this.form = new FormBuilder(Evergreen.getInstance().getFrame(), "Open Quickly");
        form.setStatusBar(status);
        form.setTypingTimeoutActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showMatches();
            }
        });
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Names Containing:", filenameField);
        formPanel.addRow("Matches:", new JScrollPane(matchList));
        form.getFormDialog().setAcceptCallable(new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() {
                openSelectedFilesFromList();
                return true;
            }
        });
        form.getFormDialog().setExtraButton(rescanButton);
    }
    
    /**
     * Sets the contents of the text field.
     */
    public void setFilenamePattern(String filenamePattern) {
        filenameField.setText(filenamePattern);
    }
    
    private class RescanAction extends AbstractAction {
        public RescanAction() {
            super("Rescan");
        }
        
        public void actionPerformed(ActionEvent e) {
            workspace.getFileList().updateFileList();
        }
    }
    
    public void fileListStateChanged(boolean isNowValid) {
        if (isNowValid) {
            showMatches();
            matchList.setEnabled(true);
            rescanButton.setEnabled(true);
        } else {
            rescanButton.setEnabled(false);
            matchList.setEnabled(false);
            setStatus(true, " ");
            switchToFakeList();
            filenameField.requestFocusInWindow();
        }
    }
    
    /**
     * Responsible for providing some visual feedback that we're rescanning.
     */
    private synchronized void switchToFakeList() {
        DefaultListModel model = new DefaultListModel();
        model.addElement("Rescan in progress...");
        matchList.setModel(model);
    }
    
    public void showDialog() {
        form.getFormDialog().setShouldRestoreFocus(true);
        form.getFormDialog().showNonModal("Open");
    }
    
    public void openSelectedFilesFromList() {
        ListModel list = matchList.getModel();
        for (int index : matchList.getSelectedIndices()) {
            openFileAtIndex(index);
        }
    }
}
