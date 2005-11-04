package e.debugger;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import e.gui.*;

/**
 * Displays a ListModel containing Breakpoints. Opens the corresponding source
 * file:line number when a cell is double-clicked.
 */

public class BreakpointList extends JList {
    
    public BreakpointList(final LocationOpener locationOpener) {
        setCellRenderer(new BreakpointListCellRenderer());
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Breakpoint b = (Breakpoint) getModel().getElementAt(getSelectedIndex());
                    if (b.isResolved()) {
                        locationOpener.openLocation(b.getLocation());
                    }
                }
            }
        });
    }
    
    /**
     * Paints invalid breakpoints in red, and unresolved ones in gray.
     */
    public static class BreakpointListCellRenderer extends EListCellRenderer {
        
        public BreakpointListCellRenderer() {
            super(true);
        }
        
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Breakpoint breakpoint = (Breakpoint) value;
            setText(breakpoint.toString());
            if (breakpoint.isResolved()) {
                setForeground(breakpoint.isValid() ? Color.BLACK : Color.RED);
            } else {
                setForeground(Color.GRAY);
            }
            return this;
        }
    }
}
