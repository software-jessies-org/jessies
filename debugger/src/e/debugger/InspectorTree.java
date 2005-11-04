package e.debugger;

import com.sun.jdi.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;

import e.gui.*;
import e.util.*;

/**
 * A tree that shows the types, names and values of local variables and fields
 * visible in a StackFrame, and allows the user to explore the contents of any
 * reference type values by expanding the tree nodes.
 */
public class InspectorTree extends ETree {
    
    public static final String SORT_FIRST = "A";
    public static final String SORT_SECOND = "B";
    public static final String SORT_LAST = "Z";
    
    public InspectorTree() {
        super(new DefaultTreeModel(null));
        setRootVisible(false);
        setShowsRootHandles(true);
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        setCellRenderer(new InspectorTreeCellRenderer());
    }
    
    /**
     * Resets the tree to show just the given StackFrame. 
     */
    public void setModel(StackFrame frame) {
        BranchNode root = new BranchNode(null);
        DefaultTreeModel model = new DefaultTreeModel(root);
        if (frame != null) {
            insertFrameInto(root, frame);
        }
        getSelectionModel().clearSelection();
        setModel(model);
    }
    
    private void insertFrameInto(BranchNode node, StackFrame frame) {
        try {
            ObjectReference thisObject = frame.thisObject();
            if (thisObject != null) {
                insertFieldsInto(node, thisObject.getValues(thisObject.referenceType().fields()));
            }
            insertLocalsInto(node, frame.getValues(frame.visibleVariables()));
        } catch (AbsentInformationException ex) {
            Log.warn("Information not available for locals.");
        }
    }
    
    private void insertFieldsInto(BranchNode node, Map<Field, Value> fields) {
        for (Field field : fields.keySet()) {
            NodeObject object = new FieldNodeObject(field, fields.get(field));
            node.add(new BranchNode(object));
        }
    }
    
    private void insertLocalsInto(BranchNode node, Map<LocalVariable, Value> locals) {
        for (LocalVariable local : locals.keySet()) {
            NodeObject object = new LocalNodeObject(local, locals.get(local));
            node.add(new BranchNode(object));
        }
    }
    
    public static class BranchNode extends DefaultMutableTreeNode {
        public BranchNode(NodeObject userObject) {
            super(userObject);
            if (userObject != null && userObject.isReferenceType()) {
                add(new BranchNode(UNEXPANDED_NODE_OBJECT));
            }
        }
        
        public void add(MutableTreeNode node) {
            NodeObject o = (NodeObject) ((BranchNode) node).getUserObject();
            insert(node, getInsertIndex(o));
        }
        
        private SortedSet<String> kidsNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        
        public int getInsertIndex(NodeObject object) {
            String insertString = object.getSortIdentifier() + kidsNames.size();
            kidsNames.add(insertString);
            return new ArrayList<String>(kidsNames).indexOf(insertString);
        }
        
        public boolean isReferenceType() {
            return ((NodeObject) getUserObject()).isReferenceType();
        }
    }
    
    public static abstract class NodeObject {
        
        private Value value;
        
        public NodeObject(Value value) {
            this.value = value;
        }
        
        public abstract String getSortIdentifier();
        
        public Value getValue() {
            return value;
        }
        
        public boolean isReferenceType() {
            return value instanceof ObjectReference;
        }
        
        public ReferenceType getReferenceType() {
            if (isReferenceType()) {
                return (ReferenceType) value.type();
            } else {
                return null;
            }
        }
    }
    
    /**
     * Marks a node which hasn't been expanded, and that should populate itself with
     * the fields from the parent node's reference type when it's shown.
     */
    private static final NodeObject UNEXPANDED_NODE_OBJECT = new NodeObject(null) {
        
        public String toString() {
            return "<ReferenceType marker>";
        }
        
        public String getSortIdentifier() {
            return SORT_LAST;
        }
     };
    
     private static class LocalNodeObject extends NodeObject {
         
        private LocalVariable local;
        
        public LocalNodeObject(LocalVariable local, Value value) {
            super(value);
            this.local = local;
        }
        
        public String toString() {
            return local.typeName() + " " + local.name();
        }
        
        public String getSortIdentifier() {
            return SORT_SECOND + (isReferenceType() ? "B" : "A") + local.name();
        }
    }
    
    private static class FieldNodeObject extends NodeObject {
        
        private Field field;
        
        public FieldNodeObject(Field field, Value value) {
            super(value);
            this.field = field;
        }
        
        public String toString() {
            return field.typeName() + " " + field.name();
        }
        
        public String getSortIdentifier() {
            return SORT_FIRST + (isReferenceType() ? "B" : "A") + field.name();
        }
     }
}
