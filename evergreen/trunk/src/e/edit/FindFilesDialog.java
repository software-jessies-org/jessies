package e.edit;

import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import e.forms.*;
import e.gui.*;
import e.util.*;

/**
 * 
 */
public class FindFilesDialog {
    private FileNamePatternField patternField = new FileNamePatternField();
    private JTextField directoryField = new JTextField("", 40);
    private JLabel status = new JLabel(" ");
    private JList matchList;
    
    private boolean haveSearched;
    private PatternSyntaxException patternSyntaxException;
    
    private Workspace workspace;
    
    private FileFinder workerThread;
    
    public class MatchingFile {
        private String name;
        private int matchCount;
        private String regularExpression;
        
        public MatchingFile(String name, int matchCount, String regularExpression) {
            this.name = name;
            this.matchCount = matchCount;
            this.regularExpression = regularExpression;
        }
        
        public void open() {
            EWindow window = Edit.openFile(workspace.getRootDirectory() + File.separator + name);
            if (window instanceof ETextWindow) {
                ETextWindow textWindow = (ETextWindow) window;
                textWindow.getText().find(regularExpression);
                textWindow.findNext();
            }
        }
        
        public String toString() {
            StringBuffer result = new StringBuffer(name);
            result.append(" (");
            result.append(matchCount);
            result.append(matchCount == 1 ? " match)" : " matches)");
            return result.toString();
        }
    }
    
    public class FileFinder extends SwingWorker {
        private DefaultListModel matchModel;
        private String regex;
        private String directory;
        private volatile boolean resultNoLongerWanted;
        
        public synchronized void doFindInDirectory(String pattern, String directory) {
            System.err.println("---doFindInDirectory " + pattern + "...");
            this.matchModel = new DefaultListModel();
            this.regex = pattern;
            this.directory = directory;
            this.resultNoLongerWanted = false;
            
            matchList.setModel(matchModel);
            status.setText("Searching...");
            start();
        }
        
        public synchronized void giveUp() {
            resultNoLongerWanted = true;
        }
        
        public Object construct() {
            System.err.println("---Starting search for " + regex + " in files matching " + directory + "...");
            Thread.currentThread().setName("Search for '" + regex + "' in " + directory);
            try {
                Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                Pattern fileNamePattern = Pattern.compile(directory, Pattern.CASE_INSENSITIVE);
                FileSearcher fileSearcher = new FileSearcher(pattern);
                List fileList = workspace.getFileList();
                String root = workspace.getRootDirectory();
                long startTime = System.currentTimeMillis();
                for (int i = 0; i < fileList.size(); i++) {
                    if (resultNoLongerWanted) {
                        System.err.println("---Aborting search!");
                        return null;
                    }
                    try {
                        String candidate = (String) fileList.get(i);
                        if (fileNamePattern.matcher(candidate).find()) {
                            int matchCount = fileSearcher.searchFile(root, candidate);
                            if (matchCount > 0) {
                                matchModel.addElement(new MatchingFile(candidate, matchCount, regex));
                            }
                        }
                    } catch (FileNotFoundException ex) {
                        ex = ex; // Not our problem.
                    }
                }
                long endTime = System.currentTimeMillis();
                System.err.println("----------took: " + (endTime - startTime) + " ms.");
            } catch (PatternSyntaxException ex) {
                patternSyntaxException = ex;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return matchModel;
        }
        
        public void finished() {
            Object result = getValue();
            workerHasFinished();
            if (resultNoLongerWanted) {
                return;
            }
            
            // This means the user can just hit Return if there's only one match.
            if (matchModel.getSize() == 1) {
                matchList.setSelectedIndex(0);
            }
            status.setText(" ");
        }
    }
    
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
        if (workerThread != null) {
            workerThread.giveUp();
        }
        
        FileFinder newWorkerThread = new FileFinder();
        newWorkerThread.doFindInDirectory(patternField.getText(), directoryField.getText());
        workerThread = newWorkerThread;
    }
    
    public synchronized void workerHasFinished() {
        workerThread = null;
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
                    MatchingFile match = (MatchingFile) matchList.getModel().getElementAt(index);
                    match.open();
                }
            }
        });
    }
    
    public FindFilesDialog(Workspace workspace) {
        this.workspace = workspace;
        this.haveSearched = false;
    }

    /**
     * Sets the contents of the text field.
     * The value null causes the pattern to stay as it was.
     */
    public void setPattern(String pattern) {
        if (pattern == null) {
            return;
        }
        patternField.setText(pattern);
    }
    
    public void setFilenamePattern(String pattern) {
        if (pattern == null) {
            return;
        }
        directoryField.setText(pattern);
    }
    
    public void showDialog() {
        if (workspace.getFileList() == null) {
            Edit.showAlert("Find Files", "The list of files for " + workspace.getTitle() + " is not yet available.");
            return;
        }
//        if (directoryField.getText().length() == 0) {
//            directoryField.setText(FileUtilities.getUserFriendlyName(workspace.getRootDirectory()));
//        }
        
        initMatchList();
        
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Files Containing:", patternField);
        formPanel.addRow("Whose Names Match:", directoryField);
        formPanel.addRow("Matches:", new JScrollPane(matchList));
        formPanel.addRow("", status);
        boolean okay = FormDialog.show(Edit.getFrame(), "Find Files", formPanel);
        
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
            MatchingFile file = (MatchingFile) list.getElementAt(indices[i]);
            file.open();
        }
    }
    
    /** Always returns true because there's no reason not to open a file. */
    public boolean isEnabled() {
        return true;
    }
}
