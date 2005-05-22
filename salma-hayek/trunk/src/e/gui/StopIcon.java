package e.gui;

import java.awt.*;
import javax.swing.*;

/**
 * This is supposed to look like a stop sign, but at the moment it's just a
 * rectangle. I'm hoping someone else will turn it into an octagon.
 */
public class StopIcon extends DrawnIcon {
    private Color color;
    
    public StopIcon(Color color) {
        super(new Dimension(14, 14));
        this.color = color;
    }
    
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(color);
        g.fillRect(x, y, getIconWidth(), getIconHeight());
        g.setColor(color.darker());
        g.drawRect(x, y, getIconWidth(), getIconHeight());
    }
}
