package e.edit;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;

import e.gui.*;

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
}
