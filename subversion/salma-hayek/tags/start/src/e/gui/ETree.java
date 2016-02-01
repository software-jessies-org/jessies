package e.gui;

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
}
