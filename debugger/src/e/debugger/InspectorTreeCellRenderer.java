package e.debugger;

import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;

public class InspectorTreeCellRenderer extends DefaultTreeCellRenderer {
     
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (value instanceof InspectorTree.BranchNode) {
            InspectorTree.NodeObject node = (InspectorTree.NodeObject) ((InspectorTree.BranchNode) value).getUserObject();
            if (node != null) {
                StringBuilder s = new StringBuilder();
                s.append(node.toString());
                if (node.isReferenceType() == false && node.getValue() != null) {
                    s.append("  ");
                    s.append(node.getValue().toString());
                    setText(s.toString());
                }
            } else {
               setText("<null>");
            }
        }
        setIcon(null);
        return this;
    }
}
