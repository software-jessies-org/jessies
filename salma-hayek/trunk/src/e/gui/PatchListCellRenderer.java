package e.gui;

import java.awt.*;
import javax.swing.*;

public class PatchListCellRenderer extends EListCellRenderer {
    public static final PatchListCellRenderer INSTANCE = new PatchListCellRenderer();
    
    /** Background color for the @@ lines. */
    private static final Color VERY_LIGHT_GRAY = new Color(230, 230, 230);
    
    /** Background color for the +++ lines. */
    private static final Color TRIPLE_PLUS_BACKGROUND = new Color(0xcc, 0xcc, 0xff);
    
    /** Background color for the --- lines. */
    private static final Color TRIPLE_MINUS_BACKGROUND = new Color(0xff, 0xcc, 0xcc);
    
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
            if (line.startsWith("+++")) {
                setBackground(TRIPLE_PLUS_BACKGROUND);
            }
        } else if (line.startsWith("-")) {
            setForeground(Color.RED);
            if (line.startsWith("---")) {
                setBackground(TRIPLE_MINUS_BACKGROUND);
            }
        } else if (line.startsWith("@@ ")) {
            setBackground(VERY_LIGHT_GRAY);
            setForeground(Color.GRAY);
        }
        return this;
    }
}
