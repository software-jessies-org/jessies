package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
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
    
    public interface ClickableTreeItem {
        public void open();
    }
    
    public class MatchingLine implements ClickableTreeItem {
        private String line;
        private File file;
        
        public MatchingLine(String line, File file) {
            this.line = line;
            this.file = file;
        }
        
        public void open() {
            EWindow window = Edit.openFile(file.toString());
            if (window instanceof ETextWindow) {
                ETextWindow textWindow = (ETextWindow) window;
                final int lineNumber = Integer.parseInt(line.substring(1, line.indexOf(':', 1)));
                textWindow.goToLine(lineNumber);
            }
        }
        
        public String toString() {
            return line;
        }
    }
    
    public class MatchingFile implements ClickableTreeItem {
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
        
        public boolean containsDefinition() {
            return containsDefinition;
        }
        
        public void open() {
            EWindow window = Edit.openFile(workspace.getRootDirectory() + File.separator + name);
            if (window instanceof ETextWindow && regularExpression != null) {
                ETextWindow textWindow = (ETextWindow) window;
                FindAction.INSTANCE.findInText(textWindow, regularExpression);
                textWindow.getText().findNext();
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
                        File file = FileUtilities.fileFromParentAndString(root, candidate);
                        if (FileUtilities.isTextFile(file) == false) {
                            // FIXME: should we do the grep(1) thing of "binary file <x> matches"?
                            continue;
                        }
                        if (regex.length() != 0) {
                            ArrayList matches = new ArrayList();
                            long t0 = System.currentTimeMillis();
                            int matchCount = fileSearcher.searchFile(root, candidate, matches);
                            long t1 = System.currentTimeMillis();
                            if (t1 - t0 > 500) {
                                System.err.println(file + " " + (t1-t0) + "ms");
                            }
                            if (matchCount > 0) {
                                DefinitionFinder definitionFinder = new DefinitionFinder(file, regex);
                                MatchingFile matchingFile = new MatchingFile(candidate, matchCount, regex, definitionFinder.foundDefinition);
                                DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(matchingFile);
                                for (int i = 0; i < matches.size(); ++i) {
                                    String line = (String) matches.get(i);
                                    fileNode.add(new DefaultMutableTreeNode(new MatchingLine(line, file)));
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
        matchView.putClientProperty("JTree.lineStyle", "None");
        
        matchView.setCellRenderer(new MatchTreeCellRenderer());
        matchView.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) matchView.getLastSelectedPathComponent();
                if (node == null) {
                    return;
                }
                
                ClickableTreeItem match = (ClickableTreeItem) node.getUserObject();
                match.open();
            }
        });
    }
    
    public class MatchTreeCellRenderer extends DefaultTreeCellRenderer {
        Font defaultFont = null;
        
        public MatchTreeCellRenderer() {
            setClosedIcon(null);
            setOpenIcon(null);
            setLeafIcon(null);
        }
        
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean isExpanded, boolean isLeaf,  int row,  boolean hasFocus) {
            Component c = super.getTreeCellRendererComponent(tree, value, isSelected, isExpanded, isLeaf, row, hasFocus);
            
            // Unlike the foreground Color, the Font gets remembered, so we
            // need to manually revert it each time.
            if (defaultFont == null) {
                defaultFont = c.getFont();
            }
            c.setFont(defaultFont);
            
            // Work around JLabel's tab-rendering stupidity.
            String text = getText();
            if (text != null && text.indexOf('\t') != -1) {
                setText(text.replaceAll("\t", "    "));
            }
            
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            if (node.getUserObject() instanceof MatchingFile) {
                MatchingFile file = (MatchingFile) node.getUserObject();
                if (file.containsDefinition()) {
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                }
            } else {
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
        
        // Register for notifications of files saved while our dialog is up.
        final SaveMonitor saveMonitor = SaveMonitor.getInstance();
        final SaveMonitor.Listener saveListener = new SaveMonitor.Listener() {
            /**
             * Update the matches if a file is saved. Ideally, we'd be a bit
             * more intelligent about this. I'd like to see a new UI that
             * doesn't build a tree and present it all at once; I'd like to
             * see matches as they're found.
             */
            public void fileSaved() {
                showMatches();
            }
        };
        saveMonitor.addSaveListener(saveListener);
        
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Files Containing:", patternField);
        formPanel.addRow("Whose Names Match:", directoryField);
        formPanel.addRow("Matches:", new JScrollPane(matchView));
        formPanel.setStatusBar(status);
        
        FormDialog formDialog = FormDialog.showNonModal(Edit.getFrame(), "Find Files", formPanel);
        
        // Remove our save listener when the dialog is dismissed.
        formDialog.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent windowEvent) {
                saveMonitor.removeSaveListener(saveListener);
            }
        });
    }
}
