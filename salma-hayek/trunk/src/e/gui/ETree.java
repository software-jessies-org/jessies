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
        doExpansion(new TreePath(getModel().getRoot()), true);
    }

    /** Collapses all the nodes in this tree. */
    public void collapseAll() {
        doExpansion(new TreePath(getModel().getRoot()), false);
    }

    /** Expands or collapses all nodes beneath the given path. */
    private void doExpansion(TreePath parent, boolean expand) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                doExpansion(path, expand);
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
        if (node.toString().toLowerCase().indexOf(string) != -1) {
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
}
