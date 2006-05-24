package e.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import e.util.*;

/**
 * A better-looking table than JTable. In particular, on Mac OS this looks
 * more like a Cocoa table than the default Aqua LAF manages.
 *
 * @author Elliott Hughes
 */
public class ETable extends JTable {
    private static final Color MAC_FOCUSED_SELECTED_CELL_HORIZONTAL_LINE_COLOR = new Color(0x7daaea);
    private static final Color MAC_UNFOCUSED_SELECTED_CELL_HORIZONTAL_LINE_COLOR = new Color(0xe0e0e0);
    
    private static final Color MAC_UNFOCUSED_SELECTED_CELL_BACKGROUND_COLOR = new Color(0xc0c0c0);
    
    private static final Color MAC_FOCUSED_UNSELECTED_VERTICAL_LINE_COLOR = new Color(0xd9d9d9);
    private static final Color MAC_FOCUSED_SELECTED_VERTICAL_LINE_COLOR = new Color(0x346dbe);
    private static final Color MAC_UNFOCUSED_UNSELECTED_VERTICAL_LINE_COLOR = new Color(0xd9d9d9);
    private static final Color MAC_UNFOCUSED_SELECTED_VERTICAL_LINE_COLOR = new Color(0xacacac);
    
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
            // Work-around for Apple 4352937.
            JLabel.class.cast(getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEADING);
            
            // Use an iTunes-style vertical-only "grid".
            setShowHorizontalLines(false);
            setShowVerticalLines(true);
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
                g.setColor(colorForRow(i));
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

    /**
     * Returns the appropriate background color for the given row.
     */
    protected Color colorForRow(int row) {
        return (row % 2 == 0) ? alternateRowColor() : getBackground();
    }
    
    private Color alternateRowColor() {
        return GuiUtilities.isGtk() ? Color.WHITE : GuiUtilities.ALTERNATE_ROW_COLOR;
    }
    
    /**
     * Shades alternate rows in different colors.
     */
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        boolean focused = hasFocus();
        boolean selected = isCellSelected(row, column);
        if (selected) {
            if (GuiUtilities.isMacOs() && focused == false) {
                // Native Mac OS renders the selection differently if the table doesn't have the focus.
                // The Mac OS LAF doesn't imitate this for us.
                c. setBackground(MAC_UNFOCUSED_SELECTED_CELL_BACKGROUND_COLOR);
                c.setForeground(UIManager.getColor("Table.foreground"));
            } else {
                c.setBackground(UIManager.getColor("Table.selectionBackground"));
                c.setForeground(UIManager.getColor("Table.selectionForeground"));
            }
        } else {
            // Outside of selected rows, we want to alternate the background color.
            c.setBackground(colorForRow(row));
            c.setForeground(UIManager.getColor("Table.foreground"));
        }
        
        if (c instanceof JComponent) {
            JComponent jc = (JComponent) c;
            
            // The Java 6 GTK LAF JCheckBox doesn't paint its background by default.
            // Sun 5043225 says this is the intended behavior, though presumably not when it's being used as a table cell renderer.
            if (GuiUtilities.isGtk() && c instanceof JCheckBox) {
                jc.setOpaque(true);
            }
            
            if (getCellSelectionEnabled() == false && isEditing() == false) {
                if (GuiUtilities.isMacOs()) {
                    // Native Mac OS doesn't draw a border on the selected cell.
                    // It does however draw a horizontal line under the whole row, and a vertical line separating each column.
                    fixMacOsCellRendererBorder(jc, selected, focused);
                } else {
                    // FIXME: doesn't Windows have row-wide selection focus?
                    // Hide the cell focus.
                    jc.setBorder(null);
                }
            }
            
            initToolTip(jc, row, column);
        }
        
        return c;
    }
    
    private void fixMacOsCellRendererBorder(JComponent renderer, boolean selected, boolean focused) {
        Border border;
        if (selected) {
            border = BorderFactory.createMatteBorder(0, 0, 1, 0, focused ? MAC_FOCUSED_SELECTED_CELL_HORIZONTAL_LINE_COLOR : MAC_UNFOCUSED_SELECTED_CELL_HORIZONTAL_LINE_COLOR);
        } else {
            border = BorderFactory.createEmptyBorder(0, 0, 1, 0);
        }
        
        // Mac OS' Aqua LAF never draws vertical grid lines, so we have to draw them ourselves.
        if (getShowVerticalLines()) {
            Color verticalLineColor;
            if (focused) {
                verticalLineColor = selected ? MAC_FOCUSED_SELECTED_VERTICAL_LINE_COLOR : MAC_FOCUSED_UNSELECTED_VERTICAL_LINE_COLOR;
            } else {
                verticalLineColor = selected ? MAC_UNFOCUSED_SELECTED_VERTICAL_LINE_COLOR : MAC_UNFOCUSED_UNSELECTED_VERTICAL_LINE_COLOR;
            }
            Border verticalBorder = BorderFactory.createMatteBorder(0, 0, 0, 1, verticalLineColor);
            border = BorderFactory.createCompoundBorder(border, verticalBorder);
        }
        
        renderer.setBorder(border);
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
