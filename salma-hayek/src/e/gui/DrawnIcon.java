package e.gui;

import java.awt.*;
import javax.swing.*;

/**
 * An 'icon' drawn by code, rather than from a bitmap.
 */
public class DrawnIcon implements Icon {
    private Dimension size;
    
    public DrawnIcon(Dimension size) {
        this.size = size;
    }
    
    public int getIconWidth() {
        return size.width;
    }
    
    public int getIconHeight() {
        return size.height;
    }
    
    /**
     * Override this to do your custom drawing.
     */
    public void paintIcon(Component c, Graphics g, int x, int y) {
    }
}
