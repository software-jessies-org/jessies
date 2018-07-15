package e.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;

public class ModernScrollBarUI extends BasicScrollBarUI {
    static class InvisibleScrollBarButton extends JButton {
        InvisibleScrollBarButton() {
            setFocusable(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setBorder(BorderFactory.createEmptyBorder());
        }
    }
    
    private static final Color DRAG_COLOR = new Color(120, 120, 120);
    private static final Color ROLL_COLOR = new Color(168, 168, 168);
    private static final Color BASE_COLOR = new Color(190, 190, 190);
    
    public ModernScrollBarUI(Color trackColor) {
        this.trackColor = trackColor;
    }
    
    public Color getTrackColor() {
        return trackColor;
    }
    
    @Override protected void configureScrollBarColors() {
        // We already set `trackColor` in the constructor.
    }
    
    @Override protected JButton createDecreaseButton(int orientation) {
        return new InvisibleScrollBarButton();
    }
    
    @Override protected JButton createIncreaseButton(int orientation) {
        return new InvisibleScrollBarButton();
    }
    
    @Override public Dimension getPreferredSize(JComponent c) {
        return new Dimension(10, 10);
    }
    
    @Override protected void paintThumb(Graphics oldGraphics, JComponent c, Rectangle r) {
        JScrollBar sb = (JScrollBar) c;
        if (r.isEmpty() || !sb.isEnabled()) return;
        
        Graphics2D g = (Graphics2D) oldGraphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(isDragging ? DRAG_COLOR : (isThumbRollover() ? ROLL_COLOR : BASE_COLOR));
        g.fillRect(r.x, r.y, r.width, r.height);
        g.dispose();
    }
}
