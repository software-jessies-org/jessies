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
        boolean useAlternateColor = (row % 2 == 0 && isCellSelected(row, column) == false);
        c.setBackground(useAlternateColor ? GuiUtilities.ALTERNATE_ROW_COLOR : getBackground());
        return c;
    }
}
