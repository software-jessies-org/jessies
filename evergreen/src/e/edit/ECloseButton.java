package e.edit;

import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class ECloseButton extends EButton implements ActionListener {
    private EWindow window;
    
    private static final Color DIRTY_COLOR = new Color(255, 0, 0);
    
    public ECloseButton(EWindow window) {
        this.window = window;
        setToolTipText(GuiUtilities.isMacOs() ? "Close (\u2318W)" : "Close (Ctrl+W)");
    }

    public void paintComponent(Graphics g) {
        g.setColor(getCrossColor());
        paintCross(g, 5, 5);
    }

    public Color getCrossColor() {
        return window.isDirty() ? getDirtyCrossColor() : getGlyphColor();
    }

    public Color getDirtyCrossColor() {
        return pressed ? DIRTY_COLOR.darker() : DIRTY_COLOR;
    }

    public void paintCross(Graphics oldGraphics, int x, int y) {
        x = GuiUtilities.scaleSizeForText(x);
        y = GuiUtilities.scaleSizeForText(y);
        final int d = GuiUtilities.scaleSizeForText(2);
        Graphics2D g = (Graphics2D) oldGraphics;
        g.setStroke(new BasicStroke(d));
        g.draw(new Line2D.Float(x, y, x * 2, y * 2));
        g.draw(new Line2D.Float(x, y * 2, x * 2, y));
    }
    
    public void actionPerformed(ActionEvent e) {
        window.closeWindow();
    }
}
