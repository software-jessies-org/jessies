package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;

import e.gui.*;
import e.util.*;

public class TagsPanel extends JPanel {
    private ETextWindow textWindow;

    private JProgressBar progressBar = new JProgressBar();
    private JPanel progressPanel;
    
    private JPanel emptyPanel;

    private JPanel detailView;

    private ETree tree;
    private Hashtable branches;
    
    public TagsPanel() {
        setLayout(new BorderLayout());
        add(createUI(), BorderLayout.CENTER);
        startFollowingFocusChanges();
    }
    
    private void startFollowingFocusChanges() {
        new KeyboardFocusMonitor() {
            public void focusChanged(Component oldOwner, Component newOwner) {
                ETextWindow newTextWindow = (ETextWindow) SwingUtilities.getAncestorOfClass(ETextWindow.class, newOwner);
                if (newTextWindow != null) {
                    ensureTagsCorrespondTo(newTextWindow);
                }
            }
        };
    }
    
    public JComponent createUI() {
        /*
         * Embed the progress bar in a panel so that on platforms where a
         * progress bar can become arbitrarily tall (everything but Mac OS X,
         * seemingly), we don't. Because it looks stupid. Unfortunately, this
         * isn't as nice as the default Mac OS X behavior, which is to center
         * the progress bar vertically.
         */
        progressPanel = new JPanel();
        progressPanel.add(progressBar);
        
        emptyPanel = new JPanel();
        emptyPanel.setBackground(UIManager.getColor("Tree.background"));
        
        detailView = new JPanel(new BorderLayout());
        tree = new ETree(new DefaultTreeModel(new DefaultMutableTreeNode("")));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getPathForLocation(e.getX(), e.getY()).getLastPathComponent();
                if (node == null) {
                    return;
                }
                TagReader.Tag tag = (TagReader.Tag) node.getUserObject();
                textWindow.goToLine(tag.lineNumber);
            }
        });
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
        renderer.setClosedIcon(null);
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);

        JScrollPane scrollPane = new JScrollPane(tree);
        detailView.add(scrollPane, BorderLayout.CENTER);

        return detailView;
    }
    
    public Workspace getWorkspace() {
        return (Workspace) SwingUtilities.getAncestorOfClass(Workspace.class, textWindow);
    }
    
    public void ensureTagsCorrespondTo(ETextWindow newTextWindow) {
        if (isShowing() == false) {
            return;
        }
        
        this.textWindow = newTextWindow;
        new TagsScanner().doScan();
    }
    
    public void ensureTagsAreHidden() {
        this.textWindow = null;
        setVisibleComponent(emptyPanel);
    }
    
    public void processTagLine(DefaultMutableTreeNode root, String line) {
    }
    
    public void setVisibleComponent(Component c) {
        removeAll();
        add(c, BorderLayout.CENTER);
        c.invalidate();
        revalidate();
        repaint();
    }

    public void showProgressBar() {
        setVisibleComponent(progressPanel);
        progressBar.setIndeterminate(true);
    }

    public void hideProgressBar() {
        setVisibleComponent(detailView);
        progressBar.setIndeterminate(false);
    }

    public class TagsScanner extends SwingWorker implements TagReader.TagListener {
        private long startTime;
        private DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        private DefaultTreeModel model = new DefaultTreeModel(root);
        private HashMap branches = new HashMap();
        {
            branches.put("", root);
        }

        public void doScan() {
            showProgressBar();
            start();
            startTime = System.currentTimeMillis();
        }

        public String getFilenameSuffix() {
            String suffix = ".txt";
            String filename = textWindow.getFilename();
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
        
        public class BranchNode extends DefaultMutableTreeNode {
            
            public BranchNode(Object userObject) {
                super(userObject);
            }
            
            public void add(MutableTreeNode node) {
                TagReader.Tag tag = (TagReader.Tag) ((DefaultMutableTreeNode) node).getUserObject();
                insert(node, getInsertIndex(tag));
            }
            
            private SortedSet kidsNames = new TreeSet();
            
            public int getInsertIndex(TagReader.Tag tag) {
                String insertString = tag.getTypeSortIndex() + tag.identifier + kidsNames.size();
                kidsNames.add(insertString);
                return new ArrayList(kidsNames).indexOf(insertString);
            }
        }
        
        public void taggingFailed(Exception ex) {
            Edit.getCurrentWorkspace().reportError("", "There was an error reading the tags (" + ex.getMessage() + ")");
        }
        
        public Object construct() {
            try {
                /*
                 * It's important to use the same suffix as the original
                 * file, because that's how ctags guesses the file's type,
                 * and hence which parser to use.
                 */
                File temporaryFile = File.createTempFile("edit-", getFilenameSuffix());
                temporaryFile.deleteOnExit();
                textWindow.writeCopyTo(temporaryFile);
                TagReader tagReader = new TagReader(temporaryFile, textWindow.getFileType(), this);
                temporaryFile.delete();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return model;
        }

        public void finished() {
            get();
            if (model == null) {
                Edit.getCurrentWorkspace().reportError("", "Couldn't make tags.");
            } else {
                tree.setModel(model);
                tree.expandAll();
            }

            hideProgressBar();

            long endTime = System.currentTimeMillis();
            double duration = ((double) (endTime - startTime)) / 1000.0;
            //Log.warn("Time taken reading tags: " + duration + "s");
        }
    }
}
