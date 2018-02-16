package e.gui;

import java.awt.*;
import javax.swing.*;
import e.util.*;

/**
 * Improves on Sun's default list cell renderer. In particular, we work
 * around a couple of mis-features in JLabel concerning the empty string,
 * and the tab character. We also offer the ability to render alternate
 * lines with different background colors.
 */
public class EListCellRenderer extends DefaultListCellRenderer {
    private boolean alternateLineColor;

    public EListCellRenderer(boolean alternateLineColor) {
        this.alternateLineColor = alternateLineColor;
    }
    
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        // Work around a couple of JLabel problems.
        String text = getText();
        if (text.length() == 0) {
            // Prevent blank lines from being squashed (mis-feature in JLabel).
            setText(" ");
        } else if (text.contains("\t")) {
            // Prevent tabs from being squashed (mis-feature in JLabel).
            setText(text.replaceAll("\t", "    "));
        }
        
        // Optionally use the line-printer paper trick of alternating row color.
        if (alternateLineColor && !isSelected) {
            setBackground(GuiUtilities.backgroundColorForRow(index));
        }
        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
            setForeground(UIManager.getColor("List.selectionForeground"));
        }
        
        setEnabled(list.isEnabled());
        
        return this;
    }
}
