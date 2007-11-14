package e.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import e.util.*;

/**
 * A better-looking table than JTable.
 * In particular, on Mac OS this looks more like a Cocoa table than the default Aqua LAF manages.
 * Likewise Linux and the GTK+ LAF.
 * We also fill the entirety of any enclosing JScrollPane by default.
 * Tool tips are automatically provided for truncated cells.
 */
public class ETable extends JTable {
    private static final Color MAC_FOCUSED_SELECTED_CELL_HORIZONTAL_LINE_COLOR = new Color(0x7daaea);
    private static final Color MAC_UNFOCUSED_SELECTED_CELL_HORIZONTAL_LINE_COLOR = new Color(0xe0e0e0);
    
    private static final Color MAC_UNFOCUSED_SELECTED_CELL_BACKGROUND_COLOR = new Color(0xc0c0c0);
    
    private static final Color MAC_FOCUSED_UNSELECTED_VERTICAL_LINE_COLOR = new Color(0xd9d9d9);
    private static final Color MAC_FOCUSED_SELECTED_VERTICAL_LINE_COLOR = new Color(0x346dbe);
    private static final Color MAC_UNFOCUSED_UNSELECTED_VERTICAL_LINE_COLOR = new Color(0xd9d9d9);
    private static final Color MAC_UNFOCUSED_SELECTED_VERTICAL_LINE_COLOR = new Color(0xacacac);
    
    /**
     * Creates a table with a default data model.
     * Callers should use setModel after construction to provide their own model; JTable's convenience constructors are not provided.
     */
    public ETable() {
        // Although it's the JTable default, most systems' tables don't draw a grid by default.
        // Worse, it's not easy (or possible?) for us to take over grid painting ourselves for those LAFs (Metal, for example) that do paint grids.
        // The Aqua and GTK LAFs ignore the grid settings anyway, so this causes no change there.
        setShowGrid(false);
        
        // Tighten the cells up, and enable the manual painting of the vertical grid lines.
        setIntercellSpacing(new Dimension());
        
        // Table column re-ordering is too badly implemented to enable.
        getTableHeader().setReorderingAllowed(false);
        
        if (GuiUtilities.isMacOs()) {
            // Work around Apple 4352937 (fixed in 10.5).
            if (System.getProperty("os.version").startsWith("10.4")) {
                ((JLabel) getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEADING);
            }
            
            // Use an iTunes-style vertical-only "grid".
            setShowHorizontalLines(false);
            setShowVerticalLines(true);
        }
        
        // Enable Java 6's row sorting by default, without yet requiring Java 6.
        try {
            java.lang.reflect.Method setAutoCreateRowSorterMethod = JTable.class.getDeclaredMethod("setAutoCreateRowSorter", new Class[] { boolean.class });
            // FIXME: this isn't safe unless callers are updated to take into account the possible view/model row index mismatches. (The selection indexes are in terms of the view, not the model.)
            //setAutoCreateRowSorterMethod.invoke(this, true);
        } catch (Exception ex) {
            // Ignore. Likely we're on Java 5, where this functionality doesn't exist.
        }
    }

    /**
     * Paints empty rows too, after letting the UI delegate do
     * its painting.
     */
    public void paint(Graphics g) {
        super.paint(g);
        paintEmptyRows(g);
    }

    /**
     * Paints the backgrounds of the implied empty rows when the
     * table model is insufficient to fill all the visible area
     * available to us. We don't involve cell renderers, because
     * we have no data.
     */
    protected void paintEmptyRows(Graphics g) {
        final int rowCount = getRowCount();
        final Rectangle clip = g.getClipBounds();
        final int height = clip.y + clip.height;
        if (rowCount * rowHeight < height) {
            for (int i = rowCount; i <= height/rowHeight; ++i) {
                g.setColor(GuiUtilities.backgroundColorForRow(i));
                g.fillRect(clip.x, i * rowHeight, clip.width, rowHeight);
            }
            
            // Mac OS' Aqua LAF never draws vertical grid lines, so we have to draw them ourselves.
            if (GuiUtilities.isMacOs() && getShowVerticalLines()) {
                g.setColor(MAC_UNFOCUSED_UNSELECTED_VERTICAL_LINE_COLOR);
                TableColumnModel columnModel = getColumnModel();
                int x = 0;
                for (int i = 0; i < columnModel.getColumnCount(); ++i) {
                    TableColumn column = columnModel.getColumn(i);
                    x += column.getWidth();
                    g.drawLine(x - 1, rowCount * rowHeight, x - 1, height);
                }
            }
        }
    }
    
    /**
     * Changes the behavior of a table in a JScrollPane to be more like
     * the behavior of JList, which expands to fill the available space.
     * JTable normally restricts its size to just what's needed by its
     * model.
     */
    public boolean getScrollableTracksViewportHeight() {
        if (getParent() instanceof JViewport) {
            JViewport parent = (JViewport) getParent();
            return (parent.getHeight() > getPreferredSize().height);
        }
        return false;
    }
    
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        return prepareComponent(super.prepareRenderer(renderer, row, column), row, column);
    }
    
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        return prepareComponent(super.prepareEditor(editor, row, column), row, column);
    }
    
    private Component prepareComponent(Component c, int row, int column) {
        boolean focused = hasFocus();
        boolean selected = isCellSelected(row, column);
        if (selected) {
            if (GuiUtilities.isMacOs() && focused == false && isEditing() == false) {
                // Native Mac OS renders the selection differently if the table doesn't have the focus.
                // The Mac OS LAF doesn't imitate this for us.
                c.setBackground(MAC_UNFOCUSED_SELECTED_CELL_BACKGROUND_COLOR);
                c.setForeground(UIManager.getColor("Table.foreground"));
            } else {
                c.setBackground(UIManager.getColor("Table.selectionBackground"));
                c.setForeground(UIManager.getColor("Table.selectionForeground"));
            }
        } else {
            // Outside of selected rows, we want to alternate the background color.
            c.setBackground(GuiUtilities.backgroundColorForRow(row));
            c.setForeground(UIManager.getColor("Table.foreground"));
        }
        
        if (c instanceof JComponent) {
            JComponent jc = (JComponent) c;
            
            if (GuiUtilities.isGtk() && c instanceof JCheckBox) {
                // The Java 6 GTK LAF JCheckBox doesn't paint its background by default.
                // Sun 5043225 says this is the intended behavior, though presumably not when it's being used as a table cell renderer.
                jc.setOpaque(true);
            } else if (GuiUtilities.isMacOs() && c instanceof JCheckBox) {
                // There's a similar situation on Mac OS.
                jc.setOpaque(true);
                // Mac OS 10.5 lets us use smaller checkboxes in table cells.
                ((JCheckBox) jc).putClientProperty("JComponent.sizeVariant", "mini");
            }
            
            if (getCellSelectionEnabled() == false && isEditing() == false) {
                if (GuiUtilities.isMacOs()) {
                    jc.setBorder(new AquaTableCellBorder(selected, focused, getShowVerticalLines()));
                } else {
                    // FIXME: doesn't Windows have row-wide selection focus?
                    // Hide the cell focus.
                    jc.setBorder(null);
                }
            }
            
            initToolTip(jc, row, column);
            c.setEnabled(this.isEnabled());
        }
        return c;
    }
    
    /**
     * Native Mac OS doesn't draw a border on the selected cell, but it does various things that we can emulate with a custom cell border.
     */
    private static class AquaTableCellBorder extends AbstractBorder {
        private boolean selected;
        private boolean focused;
        private boolean verticalLines;
        
        public AquaTableCellBorder(boolean selected, boolean focused, boolean verticalLines) {
            this.selected = selected;
            this.focused = focused;
            this.verticalLines = verticalLines;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            // Native tables draw a horizontal line under the whole selected row.
            if (selected) {
                g.setColor(focused ? MAC_FOCUSED_SELECTED_CELL_HORIZONTAL_LINE_COLOR : MAC_UNFOCUSED_SELECTED_CELL_HORIZONTAL_LINE_COLOR);
                g.drawLine(x, y + height - 1, x + width, y + height - 1);
            }
            
            // Mac OS' Aqua LAF never draws vertical grid lines, so we have to draw them ourselves.
            if (verticalLines) {
                if (focused) {
                    g.setColor(selected ? MAC_FOCUSED_SELECTED_VERTICAL_LINE_COLOR : MAC_FOCUSED_UNSELECTED_VERTICAL_LINE_COLOR);
                } else {
                    g.setColor(selected ? MAC_UNFOCUSED_SELECTED_VERTICAL_LINE_COLOR : MAC_UNFOCUSED_UNSELECTED_VERTICAL_LINE_COLOR);
                }
                g.drawLine(x + width - 1, y, x + width - 1, y + height);
            }
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            // Defer to getBorderInsets(Component c, Insets insets)...
            Insets result = new Insets(0, 0, 0, 0);
            return getBorderInsets(c, result);
        }
        
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            // FIXME: the whole reason this class exists is because Apple's LAF doesn't like insets other than these, so this might be fragile if they update the LAF.
            insets.left = insets.top = insets.right = insets.bottom = 1;
            return insets;
        }
        
        @Override
        public boolean isBorderOpaque() {
            return true;
        }
    }
    
    /**
     * Sets the component's tool tip if the component is being rendered smaller than its preferred size.
     * This means that all users automatically get tool tips on truncated text fields that show them the full value.
     */
    private void initToolTip(JComponent c, int row, int column) {
        String toolTipText = null;
        if (c.getPreferredSize().width > getCellRect(row, column, false).width) {
            toolTipText = getValueAt(row, column).toString();
        }
        c.setToolTipText(toolTipText);
    }
    
    /**
     * Places tool tips over the cell they correspond to. MS Outlook does this, and it works rather well.
     * Swing will automatically override our suggested location if it would cause the tool tip to go off the display.
     */
    @Override
    public Point getToolTipLocation(MouseEvent e) {
        // After a tool tip has been displayed for a cell that has a tool tip, cells without tool tips will show an empty tool tip until the tool tip mode times out (or the table has a global default tool tip).
        // (ToolTipManager.checkForTipChange considers a non-null result from getToolTipText *or* a non-null result from getToolTipLocation as implying that the tool tip should be displayed. This seems like a bug, but that's the way it is.)
        if (getToolTipText(e) == null) {
            return null;
        }
        final int row = rowAtPoint(e.getPoint());
        final int column = columnAtPoint(e.getPoint());
        if (row == -1 || column == -1) {
            return null;
        }
        return getCellRect(row, column, false).getLocation();
    }
    
    /**
     * Improve the appearance of of a table in a JScrollPane on Mac OS, where there's otherwise an unsightly hole.
     */
    @Override
    protected void configureEnclosingScrollPane() {
        super.configureEnclosingScrollPane();
        
        if (GuiUtilities.isMacOs() == false) {
            return;
        }
        
        Container p = getParent();
        if (p instanceof JViewport) {
            Container gp = p.getParent();
            if (gp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane)gp;
                // Make certain we are the viewPort's view and not, for
                // example, the rowHeaderView of the scrollPane -
                // an implementor of fixed columns might do this.
                JViewport viewport = scrollPane.getViewport();
                if (viewport == null || viewport.getView() != this) {
                    return;
                }
                
                // JTable copy & paste above this point; our code below.
                
                // Put a dummy header in the upper-right corner.
                final Component renderer = new JTableHeader().getDefaultRenderer().getTableCellRendererComponent(null, "", false, false, -1, 0);
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(renderer, BorderLayout.CENTER);
                scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, panel);
            }
        }
    }
}
