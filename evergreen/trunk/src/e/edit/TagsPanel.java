package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.tree.*;

import e.gui.*;
import e.util.*;

public class TagsPanel extends JPanel {
    private JProgressBar progressBar = new JProgressBar();
    private JPanel progressPanel;
    
    private JPanel emptyPanel;
    
    public TagsPanel() {
        setLayout(new BorderLayout());
        add(createUI(), BorderLayout.CENTER);
    }
    
    public void setTagsTree(Component c) {
        if (c != null) {
            setVisibleComponent(c);
        } else {
            ensureTagsAreHidden();
        }
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
        progressPanel.setBackground(UIManager.getColor("Tree.background"));
        progressPanel.add(progressBar);
        
        emptyPanel = new JPanel();
        emptyPanel.setBackground(UIManager.getColor("Tree.background"));
        return emptyPanel;
    }
    
    public void ensureTagsAreHidden() {
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
    
    public void hideProgressBar() {
        setVisibleComponent(emptyPanel);
        progressBar.setIndeterminate(false);
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
    
    public static class TagsTreeRenderer extends DefaultTreeCellRenderer {
        
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
    
    public static class TagsScanner extends SwingWorker implements TagReader.TagListener {
        private boolean tagsHaveNotChanged;
        private String digest;
        private long startTime;
        private Timer progressTimer;
        private TagsUpdater model;
        
        private DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        private DefaultTreeModel treeModel = new DefaultTreeModel(root);
        private HashMap branches = new HashMap();
        {
            branches.put("", root);
        }
        
        public TagsScanner(TagsUpdater model) {
            this.model = model;
            progressTimer = new Timer(500, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Edit.getTagsPanel().showProgressBar();
                }
            });
            progressTimer.setRepeats(false);
        }
        
        public void doScan() {
            start();
            startTime = System.currentTimeMillis();
            progressTimer.start();
        }
        
        public String getFilenameSuffix() {
            String suffix = ".txt";
            String filename = model.getTextWindow().getFilename();
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
                model.getTextWindow().writeCopyTo(temporaryFile);
                TagReader tagReader = new TagReader(temporaryFile, model.getTextWindow().getFileType(), this);
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
            
            progressTimer.stop();
            Edit.getTagsPanel().hideProgressBar();
            if (model == null) {
                Edit.getCurrentWorkspace().reportError("", "Couldn't make tags.");
            } else {
                model.setTreeModel(treeModel);
            }
            
            long endTime = System.currentTimeMillis();
            double duration = ((double) (endTime - startTime)) / 1000.0;
            //Log.warn("Time taken reading tags: " + duration + "s");
        }
    }
}
