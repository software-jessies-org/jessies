package e.debugger;

import com.sun.jdi.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

import e.gui.*;
import e.util.*;

/**
 * Displays a List<StackFrame> in a JList. opens the corresponding source code
 * loaction in the editor when a cell is double-clicked.
 */

public class StackFrameList extends JList {
    
    public StackFrameList(final LocationOpener locationOpener) {
        setCellRenderer(new StackFrameListCellRenderer());
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    StackFrame frame = (StackFrame) getModel().getElementAt(getSelectedIndex());
                    locationOpener.openLocation(frame.location());
                }
            }
        });
    }
    
    /**
     * Sets the model to a new list of frames. If the model is not empty, select the frame
     * at the top of the stack.
     */
    public void setModel(List<StackFrame> frames) {
        setModel(new StackFrameListModel(frames));
        if (frames.size() > 0) {
            getSelectionModel().setSelectionInterval(0, 0);
        }
    }
    
    public void setEnabled(boolean enabled) {
        if (enabled == false) {
            getSelectionModel().clearSelection();
            setModel(new StackFrameListModel(new ArrayList<StackFrame>()));
        }
    }
    
    public StackFrame getSelectedStackFrame() {
        int selectedRow = getSelectedIndex();
        if (selectedRow == -1) {
            return null;
        } else {
            return (StackFrame) getModel().getElementAt(selectedRow);
        }
    }
    
    private static class StackFrameListModel extends AbstractListModel {
        
        private List<StackFrame> rows;
        
        public StackFrameListModel(List<StackFrame> rows) {
            this.rows = rows;
        }
        
        public int getSize() {
            return rows.size();
        }
        
        public Object getElementAt(int row) {
            return rows.get(row);
        }
    }
    
    /**
     * Renders the Location and method name of the stack frame.
     */
    public static class StackFrameListCellRenderer extends EListCellRenderer {
        
        public StackFrameListCellRenderer() {
            super(true);
        }
        
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            StackFrame frame = (StackFrame) value;
            Location location = frame.location();
            setText(location.toString() + " " + location.method().name());
            return this;
        }
    }
}
