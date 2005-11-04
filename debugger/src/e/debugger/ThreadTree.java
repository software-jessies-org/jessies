package e.debugger;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import e.gui.*;
import e.util.*;

/**
 * A tree UI component showing all the threads and thread groups currently running in
 * the target VM. Branch nodes represent thread groups, and leaf nodes threads.
 */

public class ThreadTree extends ETree {
    
    private DefaultTreeModel model;
    private TreeSelectionModel selection;
    private Map<ThreadGroupReference, DefaultMutableTreeNode> threadGroupNodes = new HashMap<ThreadGroupReference, DefaultMutableTreeNode>();
    private Map<ThreadReference, DefaultMutableTreeNode> threadNodes = new HashMap<ThreadReference, DefaultMutableTreeNode>();
    
    public ThreadTree() {
        super(new DefaultTreeModel(new DefaultMutableTreeNode("")));
        setRootVisible(false);
        setRowHeight(-1);
        this.selection = getSelectionModel();
        selection.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }
    
    public void enableThreadEvents(final TargetVm vm) {
        VmEventQueue eventQueue = vm.getEventQueue();
        eventQueue.addVmEventListener(new VmEventListener() {
            public Class getEventClass() {
                return ThreadStartEvent.class;
            }
            public void eventDispatched(Event e) {
                setThreads(vm.getAllThreads());
                vm.resume();
            }
        });
        eventQueue.addVmEventListener(new VmEventListener() {
            public Class getEventClass() {
                return ThreadDeathEvent.class;
            }
            public void eventDispatched(Event e) {
                setThreads(vm.getAllThreads());
                vm.resume();
            }
        });
        EventRequestManager eventManager = vm.getEventRequestManager();
        ThreadStartRequest startRequest = eventManager.createThreadStartRequest();
        ThreadDeathRequest deathRequest = eventManager.createThreadDeathRequest();
        startRequest.setEnabled(true);
        deathRequest.setEnabled(true);
    }
    
    public void setEnabled(boolean enabled) {
        if (enabled) {
            setSelectionModel(selection);
        } else {
            selection.clearSelection();
            setSelectionModel(null);
        }
    }
    
    public void selectThread(ThreadReference thread) {
        DefaultMutableTreeNode node = threadNodes.get(thread);
        if (node != null) {
            TreePath path = new TreePath(model.getPathToRoot(node));
            setSelectionPath(path);
            scrollPathToVisible(path);
        }
    }
    
    public ThreadReference getSelectedThread() {
        TreePath path = getSelectionPath();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (node.isLeaf()) {
            return (ThreadReference) node.getUserObject();
        } else {
            return null;
        }
    }
    
    public void setThreads(List<ThreadReference> threads) {
        setModel(model = new DefaultTreeModel(new DefaultMutableTreeNode("")));
        threadGroupNodes.clear();
        for (ThreadReference thread : threads) {
            addThread(thread);
        }
        expandAll();
    }
    
    private void addThread(ThreadReference thread) {
        DefaultMutableTreeNode groupNode = getGroupNode(thread.threadGroup());
        DefaultMutableTreeNode threadNode = new DefaultMutableTreeNode(thread);
        threadNodes.put(thread, threadNode);
        model.insertNodeInto(threadNode, groupNode, groupNode.getChildCount());
    }
    
    /**
     * Returns the node corresponding to the given ThreadGroupReference, creating
     * it and all missing parent ThreadGroupReference nodes if necessary.
     */
    private DefaultMutableTreeNode getGroupNode(ThreadGroupReference threadGroup) {
        if (threadGroup == null) {
            return (DefaultMutableTreeNode) getModel().getRoot();
        }
        DefaultMutableTreeNode groupNode = threadGroupNodes.get(threadGroup);
        if (groupNode == null) {
            groupNode = new DefaultMutableTreeNode(threadGroup.name());
            threadGroupNodes.put(threadGroup, groupNode);
        }
        DefaultMutableTreeNode parentNode = getGroupNode(threadGroup.parent());
        model.insertNodeInto(groupNode, parentNode, 0);
        return groupNode;
    }
}
