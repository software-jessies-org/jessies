package e.gui;

import java.awt.*;
import javax.swing.*;

public class EListCellRenderer extends DefaultListCellRenderer {
    private static final Color ALTERNATE_ROW_COLOR = new Color(0.92f, 0.95f, 0.99f);
    
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        
        if (!isSelected && index % 2 == 0) {
            setBackground(ALTERNATE_ROW_COLOR);
        }
        return this;
    }
}
