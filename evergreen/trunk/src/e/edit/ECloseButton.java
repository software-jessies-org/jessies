package e.gui;

import java.awt.*;
import java.awt.event.*;

public class ECloseButton extends EButton implements ActionListener {
    private EWindow window;
    
    private static final Color DIRTY_COLOR = new Color(255, 0, 0);
    
    public ECloseButton(EWindow window) {
        super("");
        this.window = window;
        addActionListener(this);
    }

    public void paint(Graphics g) {
        g.setColor(getCrossColor());
        paintCross(g, 5, 5);
    }

    public Color getCrossColor() {
        return window.isDirty() ? getDirtyCrossColor() : getCleanCrossColor();
    }

    public Color getDirtyCrossColor() {
        return pressed ? DIRTY_COLOR.darker() : DIRTY_COLOR;
    }

    public Color getCleanCrossColor() {
        return pressed ? Color.GRAY : Color.BLACK;
    }

    public void paintCross(Graphics g, int x, int y) {
        final int d = 2;
        
        // Top left.
        g.fillRect(x, y, d, d);
        g.fillRect(x + 1, y + 1, d, d);
        
        // Top right.
        g.fillRect(x + 5, y + 1, d, d);
        g.fillRect(x + 6, y, d, d);
        
        // Center.
        g.fillRect(x + 2, y + 2, 2*d, 2*d);
        
        // Bottom left.
        g.fillRect(x, y + 6, d, d);
        g.fillRect(x + 1, y + 5, d, d);
        
        // Bottom right.
        g.fillRect(x + 5, y + 5, d, d);
        g.fillRect(x + 6, y + 6, d, d);
    }
    
    public void actionPerformed(ActionEvent e) {
        window.closeWindow();
    }
}
