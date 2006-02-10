package e.gui;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;

public class ETree extends JTree {
    /** Constructs a new tree with the given root. */
    public ETree(DefaultTreeModel model) {
        super(model);
    }

    /** Expands all the nodes in this tree. */
    public void expandAll() {
        expandOrCollapsePath(new TreePath(getModel().getRoot()), true);
    }

    /** Collapses all the nodes in this tree. */
    public void collapseAll() {
        expandOrCollapsePath(new TreePath(getModel().getRoot()), false);
    }
    
    /** Expands or collapses all nodes beneath the given path represented as an array of nodes. */
    public void expandOrCollapsePath(TreeNode[] nodes, boolean expand) {
        expandOrCollapsePath(new TreePath(nodes), expand);
    }
    
    /** Expands or collapses all nodes beneath the given path. */
    private void expandOrCollapsePath(TreePath parent, boolean expand) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandOrCollapsePath(path, expand);
            }
        }
        if (expand) {
            expandPath(parent);
        } else {
            collapsePath(parent);
        }
    }
    
    /**
     * Selects the nodes matching the given string. The matching is
     * a case-insensitive substring match. The selection is not cleared
     * first; you must do this yourself if it's the behavior you want.
     * 
     * If ensureVisible is true, the first selected node in the model
     * will be made visible via scrollPathToVisible.
     */
    public void selectNodesMatching(String string, boolean ensureVisible) {
        TreePath path = new TreePath(getModel().getRoot());
        selectNodesMatching(path, string.toLowerCase());
        if (ensureVisible) {
            scrollPathToVisible(getSelectionPath());
        }
    }
    
    private void selectNodesMatching(TreePath parent, String string) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                selectNodesMatching(path, string);
            }
        }
        if (node.toString().toLowerCase().contains(string)) {
            addSelectionPath(parent);
        }
    }
    
    /** Scrolls the path to the middle of the scroll pane. */
    public void scrollPathToVisible(TreePath path) {
        if (path == null) {
            return;
        }
        makeVisible(path);
        Rectangle pathBounds = getPathBounds(path);
        if (pathBounds != null) {
            Rectangle visibleRect = getVisibleRect();
            if (getHeight() > visibleRect.height) {
                int y = pathBounds.y - visibleRect.height / 2;
                visibleRect.y = Math.min(Math.max(0, y), getHeight() - visibleRect.height);
                scrollRectToVisible(visibleRect);
            }
        }
    }
    
    /**
     * Makes JTree's implementation less width-greedy. Left to JTree, we'll
     * grow to be wide enough to show our widest node without using a scroll
     * bar. While this is seemingly widely acceptable (ho ho), it's no good
     * in Edit's "Find in Files" dialog. If long lines match, next time you
     * open the dialog, it can be so wide it doesn't fit on the screen. Here,
     * we go for the minimum width, and assume that an ETree is never packed
     * on its own (in which case, it might end up rather narrow by default).
     */
    public Dimension getPreferredScrollableViewportSize() {
        Dimension size = super.getPreferredScrollableViewportSize();
        size.width = getMinimumSize().width;
        return size;
    }
}
