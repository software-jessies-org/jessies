package e.edit;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import org.jdesktop.swingworker.SwingWorker;

/**
 * Improves on Apple Project Builder's "Open Quickly", which just pops up a dialog where you type a name.
 * We have a list -- updated as you type in the filename field -- showing what files match the regular expression you've typed.
 * You can double-click individual entries to open them, or hit Return to open just the selected one(s).
 */
public class OpenQuicklyDialog implements WorkspaceFileList.Listener {
    private JTextField filenameField = new JTextField(40);
    private JList matchList;
    private ELabel status = new ELabel();
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
                final long t0 = System.nanoTime();
                
                List<String> fileList = workspace.getFileList().getListOfFilesMatching(regularExpression);
                for (int i = 0; i < fileList.size(); i++) {
                    model.addElement(fileList.get(i));
                }
                final int indexedFileCount = workspace.getFileList().getIndexedFileCount();
                if (indexedFileCount != -1) {
                    statusText = fileList.size() + " / " + StringUtilities.pluralize(indexedFileCount, "file", "files") + " match.";
                }
                
                final long t1 = System.nanoTime();
                Log.warn("Search for files matching \"" + regularExpression + "\" took " + TimeUtilities.nsToString(t1 - t0) + ".");
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
            matchList.setEnabled(true);
            // If we don't set the selected index, the user won't be able to cycle the focus into the list with the Tab key.
            // This also means the user can just hit Return if there's only one match.
            matchList.setSelectedIndex(0);
        }
    }
    
    public synchronized void showMatches() {
        // Only bother if the user can see the results, and we're not currently rescanning the index.
        if (matchList.isShowing() && workspace.getFileList().getIndexedFileCount() != -1) {
            new MatchFinder(filenameField.getText()).execute();
        }
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
    
    private void initMatchList() {
        matchList = new JList();
        matchList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
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
        form.setTypingTimeoutActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showMatches();
            }
        });
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Names Containing:", filenameField);
        formPanel.addWideRow(status);
        formPanel.addWideRow(new JScrollPane(matchList));
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
    
    public void fileListStateChanged(final boolean isNowValid) {
        rescanButton.setEnabled(isNowValid);
        if (isNowValid) {
            showMatches();
        } else {
            setStatus(true, " ");
            switchToFakeList();
            filenameField.requestFocusInWindow();
        }
    }
    
    /**
     * Provides some visual feedback that we're rescanning.
     */
    private synchronized void switchToFakeList() {
        DefaultListModel model = new DefaultListModel();
        model.addElement("Rescan in progress...");
        matchList.setModel(model);
        matchList.setEnabled(false);
    }
    
    public void showDialog() {
        form.getFormDialog().setShouldRestoreFocus(true);
        form.getFormDialog().showNonModal("Open");
    }
    
    public void openSelectedFilesFromList() {
        for (int index : matchList.getSelectedIndices()) {
            openFileAtIndex(index);
        }
    }
}
