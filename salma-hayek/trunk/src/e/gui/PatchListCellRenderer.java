package e.gui;

import java.awt.*;
import javax.swing.*;

public class PatchListCellRenderer extends EListCellRenderer {
    /**
     * The sole instance of this class.
     */
    public static final PatchListCellRenderer INSTANCE = new PatchListCellRenderer();
    
    /**
     * Background color for the @@ lines.
     */
    private static final Color VERY_LIGHT_GRAY = new Color(230, 230, 230);
    
    /**
     * Prevents the creation of useless instances.
     */
    private PatchListCellRenderer() {
        super(false);
    }
    
    /**
     * Renders lines from a context diff patch in colors inspired by code2html.
     */
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean isFocused) {
        super.getListCellRendererComponent(list, value, index, isSelected, isFocused);
        String line = (String) value;
        if (line.startsWith("+")) {
            setForeground(Color.BLUE);
        } else if (line.startsWith("-")) {
            setForeground(Color.RED);
        } else if (line.startsWith("@@ ")) {
            setBackground(VERY_LIGHT_GRAY);
            setForeground(Color.GRAY);
        }
        return this;
    }
}
