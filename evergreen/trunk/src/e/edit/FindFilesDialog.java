package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.tree.*;
import e.forms.*;
import e.gui.*;
import e.util.*;

/**
 * 
 */
public class FindFilesDialog {
    private CheapMonitoredField patternField = new CheapMonitoredField();
    private CheapMonitoredField directoryField = new CheapMonitoredField();
    private JLabel status = new JLabel(" ");
    private ETree matchView;
    private DefaultTreeModel matchTreeModel;
    
    private boolean haveSearched;
    
    private Workspace workspace;
    
    private FileFinder workerThread;
    
    public class MatchingFile {
        private String name;
        private int matchCount;
        private String regularExpression;
        private boolean containsDefinition;
        
        /**
         * For matches based just on filename.
         */
        public MatchingFile(String name) {
            this(name, 0, null, false);
        }
        
        /**
         * For matches based on filename and a regular expression.
         */
        public MatchingFile(String name, int matchCount, String regularExpression, boolean containsDefinition) {
            this.name = name;
            this.matchCount = matchCount;
            this.regularExpression = regularExpression;
            this.containsDefinition = containsDefinition;
        }
        
        public void open() {
            EWindow window = Edit.openFile(workspace.getRootDirectory() + File.separator + name);
            if (window instanceof ETextWindow && regularExpression != null) {
                ETextWindow textWindow = (ETextWindow) window;
                FindAction.INSTANCE.findInText(textWindow, regularExpression);
                textWindow.findNext();
            }
        }
        
        public String toString() {
            StringBuffer result = new StringBuffer(name);
            if (matchCount != 0) {
                result.append(" (");
                result.append(matchCount);
                result.append(matchCount == 1 ? " match" : " matches");
                if (containsDefinition) {
                    result.append(" including definition");
                }
                result.append(")");
            }
            return result.toString();
        }
    }
    
    public class FileFinder extends SwingWorker {
        private List fileList;
        private DefaultMutableTreeNode matchRoot;
        private String regex;
        private String directory;
        private volatile boolean resultNoLongerWanted;
        
        private int doneFileCount;
        private int totalFileCount;
        private int percentage;
        
        public synchronized void doFindInDirectory(String pattern, String directory) {
            System.err.println("---doFindInDirectory " + pattern + "...");
            this.matchRoot = new DefaultMutableTreeNode();
            this.regex = pattern;
            this.directory = directory;
            this.resultNoLongerWanted = false;
            this.doneFileCount = 0;
            this.fileList = workspace.getListOfFilesMatching(directory);
            this.totalFileCount = fileList.size();
            this.percentage = -1;
            
            matchTreeModel.setRoot(matchRoot);
            start();
        }
        
        public synchronized void giveUp() {
            resultNoLongerWanted = true;
        }
        
        public void updateStatus() {
            int newPercentage = (doneFileCount * 100) / totalFileCount;
            if (newPercentage != percentage) {
                percentage = newPercentage;
                setStatus("Searching... " + percentage + "%", false);
            }
        }
        
        public Object construct() {
            System.err.println("---Starting search for " + regex + " in files matching " + directory + "...");
            Thread.currentThread().setName("Search for '" + regex + "' in " + directory);
            try {
                Pattern pattern = Pattern.compile(regex);
                FileSearcher fileSearcher = new FileSearcher(pattern);
                String root = workspace.getRootDirectory();
                long startTime = System.currentTimeMillis();
                for (doneFileCount = 0; doneFileCount < fileList.size(); ++doneFileCount) {
                    if (resultNoLongerWanted) {
                        System.err.println("---Aborting search!");
                        return null;
                    }
                    try {
                        String candidate = (String) fileList.get(doneFileCount);
                        if (regex.length() != 0) {
                            ArrayList matches = new ArrayList();
                            int matchCount = fileSearcher.searchFile(root, candidate, matches);
                            if (matchCount > 0) {
                                DefinitionFinder definitionFinder = new DefinitionFinder(FileUtilities.fileFromParentAndString(root, candidate), regex);
                                DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(new MatchingFile(candidate, matchCount, regex, definitionFinder.foundDefinition));
                                for (int i = 0; i < matches.size(); ++i) {
                                    fileNode.add(new DefaultMutableTreeNode(matches.get(i)));
                                }
                                matchRoot.add(fileNode);
                            }
                        } else {
                            matchRoot.add(new DefaultMutableTreeNode(new MatchingFile(candidate)));
                        }
                    } catch (FileNotFoundException ex) {
                        ex = ex; // Not our problem.
                    }
                    updateStatus();
                }
                long endTime = System.currentTimeMillis();
                System.err.println("----------took: " + (endTime - startTime) + " ms.");
                
                String status = matchTreeModel.getChildCount(matchTreeModel.getRoot()) + " / " + totalFileCount + " file" + (totalFileCount != 1 ? "s" : "");
                if (workspace.getIndexedFileCount() != totalFileCount) {
                    status += " (from " + workspace.getIndexedFileCount() + ")";
                }
                status += " match.";
                setStatus(status, false);
            } catch (PatternSyntaxException ex) {
                setStatus(ex.getDescription(), true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return matchRoot;
        }
        
        public void finished() {
            Object result = getValue();
            workerHasFinished();
            if (resultNoLongerWanted) {
                return;
            }
            
            // If we don't set the selected index, the user won't be able to cycle the focus into the list with the Tab key.
            // This also means the user can just hit Return if there's only one match.
            matchView.expandAll();
//            matchView.setSelectedIndex(0);
        }
    }
    
    public static class DefinitionFinder implements TagReader.TagListener {
        public boolean foundDefinition = false;
        private Pattern pattern;
        public DefinitionFinder(File file, String regularExpression) {
            this.pattern = Pattern.compile(regularExpression);
            new TagReader(file, null, this);
        }
        public void tagFound(TagReader.Tag tag) {
            // Function prototypes and Java packages probably aren't interesting.
            if (tag.type.equals(TagReader.Tag.PROTOTYPE) == false && tag.type.equals(TagReader.Tag.PACKAGE) == false && pattern.matcher(tag.identifier).find()) {
                foundDefinition = true;
            }
        }
        public void taggingFailed(Exception ex) {
            Log.warn("Failed to use tags to check for a definition.", ex);
        }
    }
    
    public class CheapMonitoredField extends EMonitoredTextField {
        public CheapMonitoredField() {
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
        if (matchView != null) {
            return;
        }

        matchTreeModel = new DefaultTreeModel(null);
        matchView = new ETree(matchTreeModel);
        matchView.setRootVisible(false);
        matchView.setShowsRootHandles(true);
        
        matchView.setCellRenderer(new MatchTreeCellRenderer());
        matchView.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = matchView.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        Object o = node.getUserObject();
                        if (o instanceof MatchingFile) {
                            MatchingFile match = (MatchingFile) o;
                            match.open();
                        } else {
                            System.err.println(o);
                        }
                    }
                }
            }
        });
    }
    
    public class MatchTreeCellRenderer extends DefaultTreeCellRenderer {
        public MatchTreeCellRenderer() {
            setClosedIcon(null);
            setOpenIcon(null);
            setLeafIcon(null);
        }
        
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean isExpanded, boolean isLeaf,  int row,  boolean hasFocus) {
            Component c = super.getTreeCellRendererComponent(tree, value, isSelected, isExpanded, isLeaf, row, hasFocus);
            if (isLeaf) {
                c.setForeground(Color.GRAY);
            }
            return c;
        }
    }
    
    private void setStatus(String message, boolean isError) {
        status.setForeground(isError ? Color.RED : Color.BLACK);
        status.setText(message);
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
        directoryField.setText(pattern);
    }
    
    public void showDialog() {
        if (workspace.isFileListUnsuitableFor("Find Files")) {
            return;
        }
        
        initMatchList();
        
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Files Containing:", patternField);
        formPanel.addRow("Whose Names Match:", directoryField);
        formPanel.addRow("Matches:", new JScrollPane(matchView));
        formPanel.setStatusBar(status);
        
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openSelectedFilesFromList();
            }
        };
        FormDialog.showNonModal(Edit.getFrame(), "Find Files", formPanel, "Open", listener);
    }
    
    public void openSelectedFilesFromList() {
        /*
        ListModel list = matchView.getModel();
        int[] indices = matchView.getSelectedIndices();
        for (int i = 0; i < indices.length; i++) {
            MatchingFile file = (MatchingFile) list.getElementAt(indices[i]);
            file.open();
        }
        */
    }
}
