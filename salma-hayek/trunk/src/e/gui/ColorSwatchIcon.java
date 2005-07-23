package e.gui;

import java.awt.*;

/**
 * A rectangular color swatch useful for giving a preview of a color.
 */
public class ColorSwatchIcon extends DrawnIcon {
    private Color color;
    private Color borderColor;
    
    public ColorSwatchIcon(Color color, Dimension size) {
        super(size);
        this.color = color;
        this.borderColor = color.darker();
    }
    
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(color);
        g.fillRect(x, y, getIconWidth(), getIconHeight());
        g.setColor(borderColor);
        g.drawRect(x, y, getIconWidth(), getIconHeight());
    }
}
