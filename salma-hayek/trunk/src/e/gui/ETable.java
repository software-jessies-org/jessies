package e.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import e.util.*;

public class ETable extends JTable {
    public ETable() {
    }

    /**
     * Shades alternate rows.
     */
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (isCellSelected(row, column) == false) {
            c.setBackground((row % 2 == 0) ? GuiUtilities.ALTERNATE_ROW_COLOR : getBackground());
            c.setForeground(UIManager.getColor("Table.foreground"));
        } else {
            c.setBackground(UIManager.getColor("Table.selectionBackground"));
            c.setForeground(UIManager.getColor("Table.selectionForeground"));
        }
        return c;
    }
}
