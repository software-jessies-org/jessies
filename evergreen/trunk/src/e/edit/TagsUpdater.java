package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import e.gui.*;

public class TagsUpdater {
    private ETree tree;
    private JPanel uiPanel;
    private ETextWindow textWindow;
    private boolean followCaretChanges;
    private TreeModelBuilder treeModelBuilder;
    
    public TagsUpdater(ETextWindow textWindow) {
        this.textWindow = textWindow;
        this.treeModelBuilder = new TreeModelBuilder();
        createUI();
        installListeners();
    }
    
    private void installListeners() {
        final ETextArea text = textWindow.getText();
        // Rebuild tags when the document line count changes.
        text.getDocument().addDocumentListener(new DocumentListener() {
            private int lastLineCount;
            
            public void changedUpdate(DocumentEvent e) {
                // We don't care about style changes.
            }
            public void insertUpdate(DocumentEvent e) {
                update();
            }
            public void removeUpdate(DocumentEvent e) {
                update();
            }
            
            public void update() {
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
                showTags();
                selectTagAtCaret(text);
            }
        });
        text.getCaret().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (followCaretChanges) {
                    selectTagAtCaret(text);
                }
            }
        });
    }
    
    private void selectTagAtCaret(ETextArea text) {
        try {
            int lineNumber = text.getLineOfOffset(text.getCaretPosition());
            selectTreeNode(getTagForLine(lineNumber));
        } catch (BadLocationException ex) {
            ex = ex;
        }
    }
    
    private void createUI() {
        tree = new ETree(new DefaultTreeModel(new DefaultMutableTreeNode("")));
        // We can't trust JTree's row height calculation. Make it use
        // the preferred height of the cell renderer component instead.
        tree.setRowHeight(-1);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getPathForLocation(e.getX(), e.getY()).getLastPathComponent();
                if (node == null) {
                    return;
                }
                followCaretChanges = false;
                TagReader.Tag tag = (TagReader.Tag) node.getUserObject();
                textWindow.goToLine(tag.lineNumber);
            }
        });
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
    
    public void updateTags() {
        new Thread(treeModelBuilder).start();
    }
    
    public void setTreeModel(TreeModel treeModel) {
        tree.setModel(treeModel);
        tree.expandAll();
        showTags();
        selectTagAtCaret(textWindow.getText());
    }
    
    public void showTags() {
        Edit.getTagsPanel().setTagsTree(uiPanel);
    }
    
    /**
     * Returns the tree node containing the tag for the line. If there's no
     * tag for the line, finds the nearest tag before the caret. If that
     * fails, returns null.
     */
    public TreeNode getTagForLine(int lineNumber) throws BadLocationException {
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
    
    public class TreeModelBuilder implements Runnable, TagReader.TagListener {
        private boolean tagsHaveNotChanged;
        private String digest;
        private long startTime;
        private Timer progressTimer;
        private File temporaryFile;
        private boolean isRunning;
        
        private DefaultMutableTreeNode root;
        private DefaultTreeModel treeModel;
        private HashMap branches;
        
        public TreeModelBuilder() {
            progressTimer = new Timer(500, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Edit.getTagsPanel().showProgressBar();
                }
            });
            progressTimer.setRepeats(false);
        }
        
        public void run() {
            if (isRunning) {
                return;
            }
            isRunning = true;
            root = new DefaultMutableTreeNode("root");
            treeModel = new DefaultTreeModel(root);
            branches = new HashMap();
            branches.put("", root);
            startTime = System.currentTimeMillis();
            progressTimer.start();
            scanTags();
            finished();
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
            DefaultMutableTreeNode leaf = new DefaultMutableTreeNode(tag);
            
            if (tag.isContainerType()) {
                leaf = new BranchNode(tag);
                branches.put(tag.getClassQualifiedName(), leaf);
            }
            
            DefaultMutableTreeNode branch = (DefaultMutableTreeNode) branches.get(tag.containingClass);
            if (branch == null) {
                branch = new BranchNode(tag.containingClass);
                branches.put(tag.containingClass, branch);
                root.add(branch);
            }
            
            branch.add(leaf);
        }
        
        public void taggingFailed(Exception ex) {
            Edit.getCurrentWorkspace().reportError("", "There was an error reading the tags (" + ex.getMessage() + ")");
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
                getTextWindow().writeCopyTo(temporaryFile);
                TagReader tagReader = new TagReader(temporaryFile, getTextWindow().getFileType(), this);
                String newDigest = tagReader.getTagsDigest();
                tagsHaveNotChanged = newDigest.equals(digest);
                digest = newDigest;
                temporaryFile.delete();
            } catch (Exception ex) {
                Edit.getCurrentWorkspace().reportError("Tags", "Couldn't make tags.");
                ex.printStackTrace();
            }
        }
        
        public void finished() {
            if (progressTimer.isRunning() == false) {
                Edit.getTagsPanel().hideProgressBar();
            } else {
                progressTimer.stop();
                if (tagsHaveNotChanged) {
                    isRunning = false;
                    return;
                }
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setTreeModel(treeModel);
                    isRunning = false;
                }
            });
            
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
            TagReader.Tag tag = (TagReader.Tag) ((DefaultMutableTreeNode) node).getUserObject();
            insert(node, getInsertIndex(tag));
        }
        
        private SortedSet kidsNames = new TreeSet();
        
        public int getInsertIndex(TagReader.Tag tag) {
            String insertString = tag.getSortIdentifier() + kidsNames.size();
            kidsNames.add(insertString);
            return new ArrayList(kidsNames).indexOf(insertString);
        }
    }
}
