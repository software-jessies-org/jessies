package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import e.gui.*;
import e.util.*;

public class TagsPanel extends JPanel {
    private ETextWindow textWindow;
    private int lastLineCount;
    private String digest;

    private JProgressBar progressBar = new JProgressBar();
    private JPanel progressPanel;
    
    private JPanel emptyPanel;

    private JPanel detailView;

    private ETree tree;
    private Hashtable branches;
    
    private final DocumentListener documentListener = new DocumentListener() {
        public void changedUpdate(DocumentEvent e) {
            // We don't care about style changes.
        }
        public void insertUpdate(DocumentEvent e) {
            updateTags();
        }
        public void removeUpdate(DocumentEvent e) {
            updateTags();
        }
    };
    
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
        tree.setCellRenderer(new TagsTreeRenderer());

        JScrollPane scrollPane = new JScrollPane(tree);
        detailView.add(scrollPane, BorderLayout.CENTER);

        return detailView;
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
            String insertString = tag.getTypeSortIndex() + tag.identifier + kidsNames.size();
            kidsNames.add(insertString);
            return new ArrayList(kidsNames).indexOf(insertString);
        }
    }
    
    private static class TagsTreeRenderer extends DefaultTreeCellRenderer {
        
        private static final Shape CIRCLE = new java.awt.geom.Ellipse2D.Float(1, 1, 8, 8);
        private static final Shape SQUARE = new Rectangle(1, 2, 7, 7);
        private static final Shape TRIANGLE = new Polygon(new int[] { 0, 4, 8 }, new int[] { 8, 1, 8 }, 3);
        
        private static final Map TYPE_SHAPES = new HashMap();
        {
            TYPE_SHAPES.put("c", CIRCLE);
            TYPE_SHAPES.put("C", CIRCLE);
            TYPE_SHAPES.put("D", CIRCLE);
            TYPE_SHAPES.put("i", CIRCLE);
            TYPE_SHAPES.put("f", TRIANGLE);
            TYPE_SHAPES.put("m", SQUARE);
        }
        
        private final Icon icon = new DrawnIcon(new Dimension(10, 10)) {
            public void paintIcon(Component c, Graphics og, int x, int y) {
                Graphics2D g = (Graphics2D) og;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setColor(visibilityColor);
                g.translate(x, y);
                g.draw(typeMarker);
                if (tagIsStatic == false) {
                    g.fill(typeMarker);
                }
                g.translate(-x, -y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
            }
        };
        
        private Shape typeMarker;
        private Color visibilityColor;
        private boolean tagIsStatic = false;
        
        public TagsTreeRenderer() {
            setClosedIcon(null);
            setLeafIcon(null);
            setOpenIcon(null);
        }
        
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            visibilityColor = null;
            if (node.getUserObject() instanceof TagReader.Tag) {
                TagReader.Tag tag = (TagReader.Tag) node.getUserObject();
                visibilityColor = tag.visibilityColor();
                setForeground(visibilityColor == TagReader.Tag.PRIVATE ? Color.GRAY : Color.BLACK);
                typeMarker = (Shape) TYPE_SHAPES.get(String.valueOf(tag.type));
                tagIsStatic = tag.isStatic;
            }
            if (visibilityColor != null && typeMarker != null) {
                setIcon(icon);
            }
            return this;
        }
    }
    
    public Workspace getWorkspace() {
        return (Workspace) SwingUtilities.getAncestorOfClass(Workspace.class, textWindow);
    }
    
    public void ensureTagsCorrespondTo(ETextWindow newTextWindow) {
        if (isShowing() == false) {
            return;
        }
        
        if (textWindow != null) {
            textWindow.getText().getDocument().removeDocumentListener(documentListener);
        }
        
        this.textWindow = newTextWindow;
        this.lastLineCount = -1;
        textWindow.getText().getDocument().addDocumentListener(documentListener);
        updateTags();
    }
    
    private void updateTags() {
        final int newLineCount = textWindow.getText().getLineCount();
        if (lastLineCount == newLineCount) {
            return;
        }
        lastLineCount = newLineCount;
        new TagsScanner().doScan();
    }
    
    public void ensureTagsAreHidden() {
        this.textWindow = null;
        setVisibleComponent(emptyPanel);
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
    
    public void showTagsTree() {
        setVisibleComponent(detailView);
        progressBar.setIndeterminate(false);
    }

    public class TagsScanner extends SwingWorker implements TagReader.TagListener {
        private boolean tagsHaveNotChanged;
        private DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        private DefaultTreeModel model = new DefaultTreeModel(root);
        private HashMap branches = new HashMap();
        {
            branches.put("", root);
        }

        public void doScan() {
            //showProgressBar();
            start();
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
                String newDigest = tagReader.getTagsDigest();
                tagsHaveNotChanged = newDigest.equals(digest);
                digest = newDigest;
                temporaryFile.delete();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return model;
        }

        public void finished() {
            get();
            if (tagsHaveNotChanged) {
                return;
            }
            
            if (model == null) {
                Edit.getCurrentWorkspace().reportError("", "Couldn't make tags.");
            } else {
                tree.setModel(model);
                tree.expandAll();
            }
            
            showTagsTree();
        }
    }
}
