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
                getTextWindow().goToLine(tag.lineNumber);
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
        return getTextWindow().getText();
    }
    
    public void updateTags() {
        new Thread(new TreeModelBuilder()).start();
    }
    
    public void setTreeModel(TreeModel treeModel) {
        tree.setModel(treeModel);
        tree.expandAll();
        showTags();
        selectTagAtCaret(getTextArea());
    }
    
    public void showTags() {
        Edit.getInstance().getTagsPanel().setTagsTree(uiPanel);
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
        private boolean tagsHaveChanged;
        private long startTime;
        private Timer progressTimer;
        
        private DefaultMutableTreeNode root;
        private DefaultTreeModel treeModel;
        private Map<String, DefaultMutableTreeNode> branches = new HashMap<String, DefaultMutableTreeNode>();
        
        public TreeModelBuilder() {
            progressTimer = new Timer(500, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Edit.getInstance().getTagsPanel().showProgressBar();
                }
            });
            progressTimer.setRepeats(false);
        }
        
        @Override
        protected TreeModel doInBackground() {
            root = new BranchNode("root");
            treeModel = new DefaultTreeModel(root);
            branches.clear();
            branches.put("", root);
            startTime = System.currentTimeMillis();
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
            Edit.getInstance().getCurrentWorkspace().reportError("Is Exuberant ctags installed and on your path? There was an error reading the tags: " + ex.getMessage());
            Log.warn("Tag parsing failed", ex);
        }
        
        public void scanTags() {
            try {
                /*
                 * It's important to use the same suffix as the original
                 * file, because that's how ctags guesses the file's type,
                 * and hence which parser to use.
                 * Ctags writes the name of the file into its output, so use the
                 * same file each time so that md5 hashes can be compared.
                 */
                if (temporaryFile == null) {
                    temporaryFile = File.createTempFile("edit-", getFilenameSuffix());
                    temporaryFile.deleteOnExit();
                }
                getTextArea().getTextBuffer().writeToFile(temporaryFile);
                TagReader tagReader = new TagReader(temporaryFile, getTextWindow().getFileType(), this);
                String newDigest = tagReader.getTagsDigest();
                tagsHaveChanged = ! newDigest.equals(tagsDigest);
                tagsDigest = newDigest;
                temporaryFile.delete();
            } catch (Exception ex) {
                Edit.getInstance().getCurrentWorkspace().reportError("Couldn't make tags.");
                Log.warn("Tag creation failed", ex);
            }
        }
        
        @Override
        protected void done() {
            progressTimer.stop();
            showTags();
            if (tagsHaveChanged) {
                setTreeModel(treeModel);
            }
            long endTime = System.currentTimeMillis();
            double duration = ((double) (endTime - startTime)) / 1000.0;
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
