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
            List fileList = workspace.getListOfFilesMatching(regex);
            for (int i = 0; i < fileList.size(); i++) {
                model.addElement(fileList.get(i));
            }
            final int totalFileCount = workspace.getIndexedFileCount();
            setStatus(true, fileList.size() + " / " + totalFileCount + " file" + (totalFileCount != 1 ? "s" : "") + " match.");
        } catch (PatternSyntaxException ex) {
            setStatus(false, ex.getDescription());
        } catch (Exception ex) {
            ex.printStackTrace();
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
                    Edit.openFile(workspace.getRootDirectory() + File.separator + filename);
                    
                    // Wrestle focus back from the file we've just opened.
                    JDialog dialog = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, matchList);
                    dialog.toFront();
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
            workspace.updateFileList(this);
        }
        
        /**
         * Searches again when the file list has finished being updated.
         */
        public void stateChanged(ChangeEvent e) {
            showMatches();
            matchList.setEnabled(true);
            source.setEnabled(true);
            filenameField.requestFocus();
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
        if (workspace.isFileListUnsuitableFor("Open Quickly")) {
            return;
        }
        
        initMatchList();
        
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Names Containing:", filenameField);
        formPanel.addRow("Matches:", new JScrollPane(matchList));
        formPanel.setStatusBar(status);
        formPanel.setTypingTimeoutActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showMatches();
            }
        });
        boolean okay = FormDialog.show(Edit.getFrame(), "Open Quickly", formPanel, "Open", makeRescanButton());
        
        if (okay == false) {
            return;
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
