package e.edit;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import java.util.List;

public class FindInFilesDialog implements WorkspaceFileList.Listener {
    /** How our worker threads know whether they're still relevant. */
    private static final AtomicInteger currentSequenceNumber = new AtomicInteger(0);
    private static boolean shouldStillWorkOn(int sequenceNumber) {
        return currentSequenceNumber.get() == sequenceNumber;
    }
    private static void stopOutstandingWork() {
        currentSequenceNumber.incrementAndGet();
    }
    private static int nextSequenceNumber() {
        return currentSequenceNumber.incrementAndGet();
    }
    
    /** We share these between all workspaces, to make it harder to accidentally launch a denial-of-service attack against ourselves. */
    private static final ExecutorService definitionFinderExecutor = ThreadUtilities.newFixedThreadPool(8, "Find Definitions");
    
    /** Which workspace is this "Find in Files" for? */
    private final Workspace workspace;
    
    private final JTextField regexField = new JTextField(40);
    private final JTextField filenameRegexField = new JTextField(40);
    private final ELabel status = new ELabel();
    private final JButton rescanButton;
    private final ETree matchView;
    
    private final DefaultTreeModel matchTreeModel;
    
    /** Holds all the UI. The actual "dialog" is in here! */
    private final FormBuilder form;
    
    private boolean isShowing() {
        return form.getFormDialog().isShowing();
    }
    
    public interface ClickableTreeItem {
        public void open();
    }
    
    public class MatchingLine implements ClickableTreeItem {
        private String line;
        private Path file;
        private Pattern pattern;
        
        public MatchingLine(String line, Path file, Pattern pattern) {
            this.line = line;
            this.file = file;
            this.pattern = pattern;
        }
        
        public void open() {
            EWindow window = Evergreen.getInstance().openFile(file.toString());
            if (window instanceof ETextWindow) {
                ETextWindow textWindow = (ETextWindow) window;
                FindAction.INSTANCE.findInText(textWindow, PatternUtilities.toString(pattern));
                final int lineNumber = Integer.parseInt(line.substring(1, line.indexOf(':', 1)));
                textWindow.getTextArea().goToLine(lineNumber);
            }
            
            // Now we've opened a new file or switched to an already-open one, that's where focus should go when we're dismissed.
            form.getFormDialog().setShouldRestoreFocus(false);
        }
        
        @Override public String toString() {
            return line;
        }
    }
    
    public class MatchingFile implements ClickableTreeItem {
        private Path file;
        private String name;
        private int matchCount;
        private Pattern pattern;
        private boolean containsDefinition;
        
        /**
         * For matches based just on filename.
         */
        public MatchingFile(Path file, String name) {
            this(file, name, 0, null);
        }
        
        /**
         * For matches based on filename and a regular expression.
         */
        public MatchingFile(Path file, String name, int matchCount, Pattern pattern) {
            this.file = file;
            this.name = name;
            this.matchCount = matchCount;
            this.pattern = pattern;
            if (pattern != null) {
                definitionFinderExecutor.submit(new DefinitionFinder(file, pattern, this));
            }
        }
        
        public String getLastPartOfName() {
            return file.getFileName().toString();
        }
        
        public void setContainsDefinition(boolean newState) {
            this.containsDefinition = newState;
            // The check-in comment for revision 673 mentioned that JTree
            // caches node widths, and that changing the font for an individual
            // node could lead to unwanted clipping. The caching is done by
            // subclasses of javax.swing.tree.AbstractLayoutCache, and there
            // are methods called invalidatePathBounds and invalidateSizes, but
            // neither is easily accessible. We can access the first only via
            // the support for editable nodes. The latter we can access via
            // various setters. This relies on the fact that the setter
            // setRootVisible doesn't optimize out the case where the new state
            // is the same as the old state. We also need to explicitly request
            // a repaint. But this potentially fragile hack does let us work
            // around a visual glitch in the absence of an approved means of
            // invalidating the JTree UI delegate's layout cache. Having the
            // source is one of the things that makes Java great!
            GuiUtilities.invokeLater(() -> {
                matchView.setRootVisible(true);
                matchView.setRootVisible(false);
                matchView.repaint();
            });
        }
        
        public boolean containsDefinition() {
            return containsDefinition;
        }
        
        public void open() {
            EWindow window = Evergreen.getInstance().openFile(workspace.prependRootDirectory(name));
            if (window instanceof ETextWindow && pattern != null) {
                ETextWindow textWindow = (ETextWindow) window;
                FindAction.INSTANCE.findInText(textWindow, PatternUtilities.toString(pattern));
                textWindow.getTextArea().findNext();
            }
        }
        
        @Override public String toString() {
            StringBuilder result = new StringBuilder(getLastPartOfName());
            if (matchCount != 0) {
                result.append(" (");
                result.append(StringUtilities.pluralize(matchCount, "matching line", "matching lines"));
                if (containsDefinition) {
                    result.append(" including definition");
                }
                result.append(")");
            }
            return result.toString();
        }
    }
    
    public class FileFinder extends SwingWorker<DefaultMutableTreeNode, DefaultMutableTreeNode> {
        private List<String> fileList;
        private DefaultMutableTreeNode matchRoot;
        private String regex;
        private String fileRegex;
        private String errorMessage;
        
        private HashMap<String, DefaultMutableTreeNode> pathMap = new HashMap<>();
        
        private int sequenceNumber;
        
        private AtomicInteger doneFileCount;
        private AtomicInteger matchingFileCount;
        private int totalFileCount;
        private int percentage;
        
        private long startTimeNs;
        private long endTimeNs;
        
        public FileFinder() {
            this.sequenceNumber = nextSequenceNumber();
            
            this.matchRoot = new DefaultMutableTreeNode();
            this.regex = regexField.getText();
            this.fileRegex = filenameRegexField.getText();
            this.fileList = workspace.getFileList().getListOfFilesMatching(fileRegex);
            
            this.doneFileCount = new AtomicInteger(0);
            this.matchingFileCount = new AtomicInteger(0);
            this.totalFileCount = fileList.size();
            this.percentage = -1;
            
            matchTreeModel.setRoot(matchRoot);
        }
        
        private void updateStatus() {
            int newPercentage = (doneFileCount.get() * 100) / totalFileCount;
            if (newPercentage != percentage) {
                percentage = newPercentage;
                String status = makeStatusString() + " (" + percentage + "%)";
                setStatus(status, false);
            }
        }
        
        @Override
        protected DefaultMutableTreeNode doInBackground() {
            Thread.currentThread().setName("Search for \"" + regex + "\" in files matching \"" + fileRegex + "\"");
            
            startTimeNs = System.nanoTime();
            endTimeNs = 0;
            
            try {
                Pattern pattern = PatternUtilities.smartCaseCompile(regex);
                
                final int threadCount = Runtime.getRuntime().availableProcessors() + 1;
                ThreadPoolExecutor executor = (ThreadPoolExecutor) ThreadUtilities.newFixedThreadPool(threadCount, "find-in-files");
                for (String candidate : fileList) {
                    executor.execute(new FileSearchRunnable(candidate, pattern));
                }
                executor.shutdown();
                try {
                    executor.awaitTermination(3600, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    // Fine; we're still finished.
                }
                
                endTimeNs = System.nanoTime();
                Log.warn("Search for \"" + regex + "\" in files matching \"" + fileRegex + "\" took " + TimeUtilities.nsToString(endTimeNs - startTimeNs) + ".");
            } catch (PatternSyntaxException ex) {
                errorMessage = ex.getDescription();
            } catch (Exception ex) {
                Log.warn("Problem searching files for \"" + regex + "\".", ex);
            }
            return matchRoot;
        }
        
        private DefaultMutableTreeNode getPathNode(String pathname) {
            String[] pathElements = pathname.split(Pattern.quote(File.separator));
            String pathSoFar = "";
            DefaultMutableTreeNode parentNode = matchRoot;
            DefaultMutableTreeNode node = matchRoot;
            synchronized (matchView) {
                for (int i = 0; i < pathElements.length - 1; ++i) {
                    pathSoFar += pathElements[i] + File.separator;
                    node = pathMap.get(pathSoFar);
                    if (node == null) {
                        node = new DefaultMutableTreeNode(pathElements[i] + File.separator);
                        insertNodeInAlphabeticalOrder(parentNode, node);
                        pathMap.put(pathSoFar, node);
                    }
                    parentNode = node;
                }
            }
            return node;
        }
        
        @Override
        protected void process(List<DefaultMutableTreeNode> treeNodes) {
            if (!shouldStillWorkOn(sequenceNumber)) {
                return;
            }
            
            synchronized (matchView) {
                for (DefaultMutableTreeNode node : treeNodes) {
                    // Avoid accidental searches locking up the EDT for literally hours.
                    if (matchView.getRowCount() > 10 * 1000) {
                        return;
                    }
                    // I've no idea why new nodes default to being collapsed.
                    matchView.expandOrCollapsePath(node.getPath(), true);
                }
            }
        }
        
        @Override
        protected void done() {
            if (!shouldStillWorkOn(sequenceNumber)) {
                return;
            }
            
            if (errorMessage != null) {
                setStatus(errorMessage, true);
            } else {
                setStatus(makeStatusString(), false);
            }
        }
        
        private String makeStatusString() {
            String status = matchingFileCount.get() + " / " + StringUtilities.pluralize(totalFileCount, "file", "files");
            int indexedFileCount = workspace.getFileList().getIndexedFileCount();
            if (indexedFileCount != -1 && indexedFileCount != totalFileCount) {
                status += " (from " + indexedFileCount + ")";
            }
            status += " match.";
            if (endTimeNs != 0) {
                status += " Took " + TimeUtilities.nsToString(endTimeNs - startTimeNs) + ".";
            }
            return status;
        }
        
        private class FileSearchRunnable implements Runnable {
            private String candidate;
            private Pattern pattern;
            
            private FileSearchRunnable(String candidate, Pattern pattern) {
                this.candidate = candidate;
                this.pattern = pattern;
            }
            
            public void run() {
                if (!shouldStillWorkOn(sequenceNumber)) {
                    return;
                }
                try {
                    final long t0 = System.nanoTime();
                    FileSearcher fileSearcher = new FileSearcher(pattern);
                    Path file = FileUtilities.pathFrom(workspace.getRootDirectory(), candidate);
                    
                    // Update our percentage-complete status, but only if we've
                    // taken enough time for the user to start caring, so we
                    // don't make quick searches unnecessarily slow.
                    doneFileCount.incrementAndGet();
                    if (TimeUtilities.nsToS(t0 - startTimeNs) > 0.3) {
                        updateStatus();
                    }
                    
                    if (regex.length() != 0) {
                        ArrayList<String> matches = new ArrayList<>();
                        boolean wasText = fileSearcher.searchFile(file, matches);
                        if (wasText == false) {
                            // FIXME: should we do the grep(1) thing of "binary file <x> matches"?
                            return;
                        }
                        final long t1 = System.nanoTime();
                        if (TimeUtilities.nsToS(t1 - t0) > 0.5) {
                            Log.warn("Searching file \"" + file + "\" for \"" + regex + "\" took " + TimeUtilities.nsToString(t1 - t0) + "!");
                        }
                        final int matchCount = matches.size();
                        if (matchCount > 0) {
                            synchronized (matchView) {
                                DefaultMutableTreeNode pathNode = getPathNode(candidate);
                                MatchingFile matchingFile = new MatchingFile(file, candidate, matchCount, pattern);
                                DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(matchingFile);
                                for (String line : matches) {
                                    fileNode.add(new DefaultMutableTreeNode(new MatchingLine(line, file, pattern)));
                                }
                                insertNodeInAlphabeticalOrder(pathNode, fileNode);
                                matchingFileCount.incrementAndGet();
                                // Make sure the new node gets expanded.
                                publish(fileNode);
                            }
                        }
                    } else {
                        synchronized (matchView) {
                            DefaultMutableTreeNode pathNode = getPathNode(candidate);
                            DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(new MatchingFile(file, candidate));
                            insertNodeInAlphabeticalOrder(pathNode, fileNode);
                            matchingFileCount.incrementAndGet();
                            publish(pathNode);
                        }
                    }
                } catch (FileNotFoundException ignored) {
                    // This special case is worthwhile if your workspace's index is out of date.
                    // A common case is when the index contains generated files that may be removed during a build.
                } catch (Throwable th) {
                    Log.warn("FileSearchRunnable.call caught something", th);
                }
            }
        }
    }
    
    public static class DefinitionFinder implements Runnable, TagReader.TagListener {
        private final Path file;
        private final MatchingFile matchingFile;
        private final Pattern pattern;
        private final int sequenceNumber;
        
        public DefinitionFinder(Path file, Pattern pattern, MatchingFile matchingFile) {
            this.file = file;
            this.matchingFile = matchingFile;
            this.pattern = pattern;
            this.sequenceNumber = currentSequenceNumber.get();
        }
        
        public void run() {
            if (!shouldStillWorkOn(sequenceNumber)) {
                return;
            }
            // FIXME: obviously not all files are really UTF-8.
            new TagReader(file, null, "UTF-8", this);
        }
        
        public void tagFound(TagReader.Tag tag) {
            // Function prototypes and Java packages probably aren't interesting.
            if (tag.type == TagType.PROTOTYPE) {
               return;
            }
            if (tag.type == TagType.PACKAGE) {
               return;
            }
            if (pattern.matcher(tag.identifier).find()) {
                matchingFile.setContainsDefinition(true);
            }
        }
        
        public void taggingFailed(Exception ex) {
            Log.warn("Failed to use ctags(1) to check for a definition.", ex);
        }
    }
    
    public synchronized void showMatches() {
        // Only bother if the user can see the results, and we're not currently rescanning the index.
        if (matchView.isShowing() && workspace.getFileList().getIndexedFileCount() != -1) {
            new Thread(new FileFinder(), "Find in Files for " + workspace.getWorkspaceName()).start();
        }
    }
    
    private void initMatchList() {
        matchView.setRootVisible(false);
        matchView.setShowsRootHandles(true);
        matchView.putClientProperty("JTree.lineStyle", "None");
        
        // Set a custom cell renderer, and tell the tree that all cells have the same height to improve performance.
        matchView.setCellRenderer(new MatchTreeCellRenderer());
        TreeCellRenderer renderer = matchView.getCellRenderer();
        JComponent rendererComponent = (JComponent) renderer.getTreeCellRendererComponent(matchView, new DefaultMutableTreeNode(new MatchingLine("Hello", null, null)), true, true, true, 0, true);
        matchView.setRowHeight(rendererComponent.getPreferredSize().height);
        matchView.setLargeModel(true);
        
        matchView.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) matchView.getLastSelectedPathComponent();
                if (node == null) {
                    return;
                }
                
                // Directories are currently represented by String objects.
                Object userObject = node.getUserObject();
                if (userObject instanceof ClickableTreeItem) {
                    ClickableTreeItem match = (ClickableTreeItem) node.getUserObject();
                    match.open();
                }
            }
        });
        
        ComponentUtilities.divertPageScrollingFromTo(regexField, matchView);
        ComponentUtilities.divertPageScrollingFromTo(filenameRegexField, matchView);
    }
    
    /**
     * Ensure that newNode is inserted at the correct index in parentNode to preserve case-insensitive alphabetical ordering.
     * We originally just used DefaultMutableTreeNode.add, but that doesn't work when multiple threads are returning matches in no particular order.
     * FIXME: given that on contemporary hardware matches come back "roughly" in order, should we search for the insertion position from the back?
     */
    private void insertNodeInAlphabeticalOrder(DefaultMutableTreeNode parentNode, DefaultMutableTreeNode newNode) {
        // Find the index to insert at.
        // We compare strings, because the tree nodes are a mixture of Strings (for directories) and MatchingFiles (for matching files).
        final String newValueString = newNode.getUserObject().toString();
        int insertionIndex = 0;
        while (insertionIndex < parentNode.getChildCount()) {
            DefaultMutableTreeNode thisNode = (DefaultMutableTreeNode) parentNode.getChildAt(insertionIndex);
            String thisValueString = thisNode.getUserObject().toString();
            if (String.CASE_INSENSITIVE_ORDER.compare(thisValueString, newValueString) >= 0) {
                break;
            }
            ++insertionIndex;
        }
        // Insert it, and make sure the model understands what we did.
        parentNode.insert(newNode, insertionIndex);
        matchTreeModel.nodesWereInserted(parentNode, new int[] { insertionIndex });
    }
    
    public void fileListStateChanged(final boolean isNowValid) {
        if (isNowValid) {
            showMatches();
            matchView.setEnabled(true);
        } else {
            switchToFakeTree();
            matchView.setEnabled(false);
        }
    }
    
    public void fileCreated(String filename) {
        if (!isShowing() || !isDesiredFilename(filename)) {
            return;
        }
        try {
            String regex = regexField.getText();
            Pattern pattern = null;
            boolean addNode = false;
            ArrayList<String> matches = new ArrayList<>();
            Path file = FileUtilities.pathFrom(workspace.getRootDirectory(), filename);
            if (regex.equals("")) {
                addNode = true;
            } else {
                pattern = PatternUtilities.smartCaseCompile(regex);
                FileSearcher fileSearcher = new FileSearcher(pattern);
                boolean wasText = fileSearcher.searchFile(file, matches);
                if (wasText == false) {
                    // FIXME: should we do the grep(1) thing of "binary file <x> matches"?
                    return;
                }
                addNode = !matches.isEmpty();
            }
            if (!addNode) {
                return;
            }
            String[] pathElements = filename.split(Pattern.quote(File.separator));
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) matchTreeModel.getRoot();
            TreePath pathToExpand = new TreePath(node);
            for (int i = 0; i < pathElements.length; i++) {
                String name = pathElements[i];
                boolean isLeaf = i == (pathElements.length - 1);
                if (!isLeaf) {
                    name += File.separator;
                }
                int childIndex = findChildWithName(node, name);
                if (childIndex == -1) {
                    // Not there yet - invent a new node of the relevant type.
                    DefaultMutableTreeNode newNode = null;
                    if (isLeaf) {
                        if (matches.isEmpty()) {
                            newNode = new DefaultMutableTreeNode(new MatchingFile(file, filename));
                        } else {
                            newNode = new DefaultMutableTreeNode(new MatchingFile(file, filename, matches.size(), pattern));
                        }
                    } else {
                        newNode = new DefaultMutableTreeNode(name);
                    }
                    insertNodeInAlphabeticalOrder(node, newNode);
                    node = newNode;
                } else {
                    node = (DefaultMutableTreeNode) node.getChildAt(childIndex);
                }
                pathToExpand = pathToExpand.pathByAddingChild(node);
            }
            // By this point, node points to the properly created filename node. If we have matches (so there
            // was really a regexp and we're not merely listing files in a dir hierarchy), then add all the matches here.
            for (String line : matches) {
                node.add(new DefaultMutableTreeNode(new MatchingLine(line, file, pattern)));
            }
            int[] indices = new int[matches.size()];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = i;
            }
            matchTreeModel.nodesWereInserted(node, indices);
            // Newly-added nodes default to closed; we definitely want them to be expanded, to draw
            // attention to the change in matches.
            for (; pathToExpand.getPathCount() > 1; pathToExpand = pathToExpand.getParentPath()) {
                matchView.expandPath(pathToExpand);
            }
        } catch (IOException ex) {
            Log.warn("Failed to match in file " + filename, ex);
        }
    }
    
    public void fileChanged(String filename) {
        fileDeleted(filename);
        fileCreated(filename);
    }
    
    public void fileDeleted(String filename) {
        if (!isShowing() || !isDesiredFilename(filename)) {
            return;
        }
        String[] pathElements = filename.split(Pattern.quote(File.separator));
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) matchTreeModel.getRoot();
        DefaultMutableTreeNode toRemove = null;
        for (int i = 0; i < pathElements.length; i++) {
            String name = pathElements[i];
            if (i < pathElements.length - 1) {
                name += File.separator;
            }
            int childIndex = findChildWithName(node, name);
            if (childIndex == -1) {
                // Wasn't a match - nothing to delete.
                return;
            }
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(childIndex);
            if (node.getChildCount() > 1 || toRemove == null) {
                toRemove = childNode;
            }
            node = childNode;
        }
        // If we get here, we're pointing to the node that corresponds to the file (which clearly had matches).
        // Delete the lot.
        if (toRemove != null) {
            matchTreeModel.removeNodeFromParent(toRemove);
        }
    }
    
    private int findChildWithName(DefaultMutableTreeNode node, String name) {
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode sub = (DefaultMutableTreeNode) node.getChildAt(i);
            String subName = null;
            Object subObj = sub.getUserObject();
            if (subObj instanceof String) {
                subName = (String) subObj;
            } else if (subObj instanceof MatchingFile) {
                subName = ((MatchingFile) subObj).getLastPartOfName();
            }
            if (name.equals(subName)) {
                return i;
            }
        }
        return -1;
    }
    
    private boolean isDesiredFilename(String filename) {
        return PatternUtilities.smartCaseCompile(filenameRegexField.getText()).matcher(filename).find();
    }
    
    private void switchToFakeTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        root.add(new DefaultMutableTreeNode("Rescan in progress..."));
        matchTreeModel.setRoot(root);
    }
    
    public static class MatchTreeCellRenderer extends DefaultTreeCellRenderer {
        private Font defaultFileFont = null;
        private Font defaultCodeFont = null;
        
        public MatchTreeCellRenderer() {
            setClosedIcon(null);
            setOpenIcon(null);
            setLeafIcon(null);
        }
        
        @Override public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean isExpanded, boolean isLeaf,  int row,  boolean hasFocus) {
            Component c = super.getTreeCellRendererComponent(tree, value, isSelected, isExpanded, isLeaf, row, hasFocus);
            
            // Unlike the foreground Color, the Font gets remembered, so we
            // need to manually revert it each time.
            if (defaultCodeFont == null) {
                Preferences preferences = Evergreen.getInstance().getPreferences();
                boolean fixed = preferences.getBoolean(EvergreenPreferences.ALWAYS_USE_FIXED_FONT);
                defaultCodeFont = fixed ? preferences.getFont(EvergreenPreferences.FIXED_FONT)
                                        : preferences.getFont(EvergreenPreferences.PROPORTIONAL_FONT);
            }
            if (defaultFileFont == null) {
                Preferences preferences = Evergreen.getInstance().getPreferences();
                defaultFileFont = preferences.getFont(EvergreenPreferences.PROPORTIONAL_FONT);
            }
            
            // Work around JLabel's tab-rendering stupidity.
            String text = getText();
            if (text != null && text.contains("\t")) {
                setText(text.replaceAll("\t", "    "));
            }
            
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            c.setFont((node.getUserObject() instanceof MatchingLine) ? defaultCodeFont : defaultFileFont);
            if (node.getUserObject() instanceof MatchingFile) {
                MatchingFile file = (MatchingFile) node.getUserObject();
                if (file.containsDefinition()) {
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                }
            } else {
                c.setForeground(Color.GRAY);
            }
            
            c.setEnabled(tree.isEnabled());
            
            return c;
        }
    }
    
    private void setStatus(final String message, final boolean isError) {
        GuiUtilities.invokeLater(() -> {
            status.setForeground(isError ? Color.RED : Color.BLACK);
            status.setText(message);
        });
    }
    
    public FindInFilesDialog(Workspace workspace) {
        this.workspace = workspace;
        this.rescanButton = RescanWorkspaceAction.makeRescanButton(workspace);
        this.matchTreeModel = new DefaultTreeModel(null);
        this.matchView = new ETree(matchTreeModel);
        this.form = new FormBuilder(Evergreen.getInstance().getFrame(), "Find in Files in " + workspace.getWorkspaceName());
        
        initMatchList();
        initForm();
        workspace.getFileList().addFileListListener(this);
    }
    
    private void initForm() {
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Files Containing:", regexField);
        formPanel.addRow("Whose Names Match:", filenameRegexField);
        formPanel.addWideRow(PatternUtilities.addRegularExpressionHelpToComponent(status));
        formPanel.addWideRow(new JScrollPane(matchView));
        form.setTypingTimeoutActionListener((e) -> { showMatches(); });
        form.getFormDialog().setExtraButton(rescanButton);
        form.getFormDialog().setCancelRunnable(() -> { stopOutstandingWork(); });
        form.getFormDialog().setAcceptCallable(new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() {
                stopOutstandingWork();
                return true;
            }
        });
    }
    
    /**
     * Sets the contents of the text field.
     * The value null causes the pattern to stay as it was.
     */
    public void setPattern(String pattern) {
        if (pattern == null) {
            return;
        }
        regexField.setText(pattern);
    }
    
    public void setFilenamePattern(String pattern) {
        filenameRegexField.setText(pattern);
    }
    
    public synchronized void showDialog() {
        form.showNonModal();
    }
}
