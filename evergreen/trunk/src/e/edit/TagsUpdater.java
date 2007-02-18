package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.tree.*;
import org.jdesktop.swingworker.SwingWorker;
import e.gui.*;
import e.ptextarea.*;
import e.util.*;

public class TagsUpdater {
    private static final ExecutorService executorService = ThreadUtilities.newSingleThreadExecutor("Tags Updater");
    private static int latestSerialNumber = 0;
    private ETree tree;
    private JPanel uiPanel;
    private ETextWindow textWindow;
    private boolean followCaretChanges;
    private File temporaryFile;
    private String tagsDigest;
    
    public TagsUpdater(ETextWindow textWindow) {
        this.textWindow = textWindow;
        createUI();
        installListeners();
    }
    
    private void installListeners() {
        final ETextArea text = getTextArea();
        // Rebuild tags when the document line count changes.
        text.getTextBuffer().addTextListener(new PTextListener() {
            private int lastLineCount;
            
            public void textCompletelyReplaced(PTextEvent e) {
                update();
            }
            
            public void textRemoved(PTextEvent e) {
                update();
            }
            
            public void textInserted(PTextEvent e) {
                update();
            }
            
            public void update() {
                // FIXME: shouldn't this be testing whether e.getCharacters()
                // contains a '\n' instead of counting lines?
                int newLineCount = text.getLineCount();
                if (lastLineCount == newLineCount) {
                    return;
                }
                lastLineCount = newLineCount;
                updateTags();
            }
        });
        // Select the corresponding tag in the tree when the focus is gained or the caret moves.
        text.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                followCaretChanges = true;
                updateTags();
                showTags();
                selectTagAtCaret(text);
            }
        });
        text.addCaretListener(new PCaretListener() {
            public void caretMoved(PTextArea textArea, int oldOffset, int newOffset) {
                if (followCaretChanges) {
                    selectTagAtCaret(text);
                }
            }
        });
    }
    
    private void selectTagAtCaret(ETextArea text) {
        // FIXME - selection
        int lineNumber = text.getLineOfOffset(text.getSelectionStart());
        selectTreeNode(getTagForLine(lineNumber));
    }
    
    private void createUI() {
        tree = new ETree(new DefaultTreeModel(new DefaultMutableTreeNode("")));
        ToolTipManager.sharedInstance().registerComponent(tree);
        // We can't trust JTree's row height calculation. Make it use
        // the preferred height of the cell renderer component instead.
        tree.setRowHeight(-1);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        
        // Make both space and enter in the tree jump to the currently selected tag, and transfer focus back to the file.
        tree.getActionMap().put("select-tag", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                getTextArea().goToLine(((TagReader.Tag) ((DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent()).getUserObject()).lineNumber);
                getTextArea().requestFocusInWindow();
            }
        });
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "select-tag");
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "select-tag");
        
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node == null) {
                    return;
                }
                followCaretChanges = false;
                TagReader.Tag tag = (TagReader.Tag) node.getUserObject();
                getTextArea().goToLine(tag.lineNumber);
            }
        });
        tree.setFont(UIManager.getFont("TableHeader.font"));
        tree.setCellRenderer(new TagsPanel.TagsTreeRenderer());
        
        final SearchField searchField = new SearchField("Search Symbols");
        searchField.setSendsNotificationForEachKeystroke(true);
        searchField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tree.clearSelection();
                String searchTerm = searchField.getText();
                if (searchTerm.length() > 0) {
                    tree.selectNodesMatching(searchTerm, true);
                }
            }
        });
        
        uiPanel = new JPanel(new BorderLayout());
        uiPanel.add(searchField, BorderLayout.NORTH);
        uiPanel.add(new JScrollPane(tree), BorderLayout.CENTER);
    }
    
    public ETextWindow getTextWindow() {
        return textWindow;
    }
    
    public ETextArea getTextArea() {
        return getTextWindow().getTextArea();
    }
    
    public void updateTags() {
        int serialNumber = ++latestSerialNumber;
        executorService.execute(new TreeModelBuilder(serialNumber));
    }
    
    public void setTreeModel(TreeModel treeModel) {
        tree.setModel(treeModel);
        tree.expandAll();
        showTags();
        selectTagAtCaret(getTextArea());
    }
    
    public void showTags() {
        Evergreen.getInstance().getTagsPanel().setTagsTree(uiPanel);
    }
    
    /**
     * Returns the tree node containing the tag for the line. If there's no
     * tag for the line, finds the nearest tag before the caret. If that
     * fails, returns null.
     */
    public TreeNode getTagForLine(int lineNumber) {
        lineNumber++; // JTextComponent numbers lines from 0, ectags from 1.
        
        TagReader.Tag nearestTag = null;
        TreeNode nearestNode = null;
        
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        for (Enumeration e = root.breadthFirstEnumeration(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.getUserObject() instanceof TagReader.Tag) {
                TagReader.Tag tag = (TagReader.Tag) node.getUserObject();
                if (tag.lineNumber == lineNumber) {
                    return node;
                } else if (tag.lineNumber < lineNumber) {
                    if (nearestTag == null || tag.lineNumber > nearestTag.lineNumber) {
                        nearestTag = tag;
                        nearestNode = node;
                    }
                }
            }
        }
        return nearestNode;
    }
    
    public void selectTreeNode(TreeNode node) {
        if (node == null) {
            tree.clearSelection();
        } else {
            DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
            TreePath path = new TreePath(treeModel.getPathToRoot(node));
            tree.setSelectionPath(path);
            if (followCaretChanges) {
                tree.scrollPathToVisible(path);
            }
        }
    }
    
    public class TreeModelBuilder extends SwingWorker<TreeModel, TagReader.Tag> implements TagReader.TagListener {
        private int serialNumber;
        private boolean tagsHaveChanged;
        private boolean successful = true;
        private long startTime;
        private Timer progressTimer;
        
        private DefaultMutableTreeNode root;
        private DefaultTreeModel treeModel;
        private Map<String, DefaultMutableTreeNode> branches = new HashMap<String, DefaultMutableTreeNode>();
        
        public TreeModelBuilder(int serialNumber) {
            this.serialNumber = serialNumber;
        }
        
        @Override
        protected TreeModel doInBackground() {
            // Don't waste time during start-up, and don't waste time if we're already out of date before we start.
            Evergreen.getInstance().awaitInitialization();
            if (serialNumber != latestSerialNumber) {
                return null;
            }
            
            root = new BranchNode("root");
            treeModel = new DefaultTreeModel(root);
            branches.clear();
            branches.put("", root);
            startTime = System.currentTimeMillis();
            progressTimer = new Timer(500, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Evergreen.getInstance().getTagsPanel().showProgressBar();
                }
            });
            progressTimer.setRepeats(false);
            progressTimer.start();
            scanTags();
            return treeModel;
        }
        
        public String getFilenameSuffix() {
            String suffix = ".txt";
            String filename = getTextWindow().getFilename();
            int lastDot = filename.lastIndexOf('.');
            if (lastDot != -1) {
                suffix = filename.substring(lastDot);
            }
            return suffix;
        }
        
        public void tagFound(TagReader.Tag tag) {
            publish(tag);
        }
        
        @Override
        protected void process(TagReader.Tag... tags) {
            for (TagReader.Tag tag : tags) {
                tag.toolTip = getTextArea().getLineText(tag.lineNumber - 1).trim();
                
                DefaultMutableTreeNode leaf = new DefaultMutableTreeNode(tag);
                
                if (tag.type.isContainer()) {
                    leaf = new BranchNode(tag);
                    branches.put(tag.getClassQualifiedName(), leaf);
                }
                
                DefaultMutableTreeNode branch = branches.get(tag.containingClass);
                if (branch == null) {
                    branch = new BranchNode(tag.containingClass);
                    branches.put(tag.containingClass, branch);
                    root.add(branch);
                }
                
                branch.add(leaf);
            }
        }
        
        public void taggingFailed(Exception ex) {
            successful = false;
            Evergreen.getInstance().getTagsPanel().showError("<b>Is Exuberant ctags installed and on your path?</b><p>There was an error reading the tags: " + ex.getMessage());
        }
        
        public void scanTags() {
            try {
                // Ctags writes the name of the file into its output, so we need to use the same file each time so that md5 hashes can be compared.
                if (temporaryFile == null) {
                    // It's important to use the same suffix as the original file, because that's how ctags guesses the file's type, and hence which parser to use.
                    temporaryFile = File.createTempFile("e.edit.TagsUpdater-", getFilenameSuffix());
                    temporaryFile.deleteOnExit();
                }
                getTextArea().getTextBuffer().writeToFile(temporaryFile);
                TagReader tagReader = new TagReader(temporaryFile, getTextWindow().getFileType(), tagsDigest, this);
                String newDigest = tagReader.getTagsDigest();
                tagsHaveChanged = (newDigest != null) && (newDigest.equals(tagsDigest) == false);
                tagsDigest = newDigest;
                // See the comment above for why we don't delete our temporary files, except on exit.
                //temporaryFile.delete();
            } catch (Exception ex) {
                Evergreen.getInstance().getTagsPanel().showError("Couldn't make tags: " + ex.getMessage());
                Log.warn("Couldn't make tags", ex);
            }
        }
        
        @Override
        protected void done() {
            if (progressTimer != null) {
                progressTimer.stop();
            }
            if (successful) {
                showTags();
                if (tagsHaveChanged && serialNumber == latestSerialNumber) {
                    setTreeModel(treeModel);
                }
            }
            //long endTime = System.currentTimeMillis();
            //double duration = ((double) (endTime - startTime)) / 1000.0;
            //Log.warn("Time taken reading tags: " + duration + "s");
        }
    }
    
    private static class BranchNode extends DefaultMutableTreeNode {
        public BranchNode(Object userObject) {
            super(userObject);
        }
        
        public void add(MutableTreeNode node) {
            Object o = ((DefaultMutableTreeNode) node).getUserObject();
            if (o instanceof TagReader.Tag) {
                insert(node, getInsertIndex((TagReader.Tag) o));
            } else {
                super.add(node);
            }
        }
        
        private SortedSet<String> kidsNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        
        public int getInsertIndex(TagReader.Tag tag) {
            String insertString = tag.getSortIdentifier() + kidsNames.size();
            kidsNames.add(insertString);
            return new ArrayList<String>(kidsNames).indexOf(insertString);
        }
    }
}
