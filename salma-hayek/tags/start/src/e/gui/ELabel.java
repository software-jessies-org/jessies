package e.gui;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

/**
A simple label. 
*/
public class ELabel extends JComponent {
    /** The label. */
    private String label;
    
    /** Creates a new label. */
    public ELabel(String label) {
        setFont(new Font("palatino", Font.PLAIN, 13));
        setLabel(label);
    }
    
    /** Sets the text for this label. */
    public void setLabel(String label) {
        this.label = label;
        repaint();
    }
    
    /** Gets the text for this label. */
    public String getLabel() {
        return label;
    }
    
    public void paint(Graphics g) {
        paintText(g);
    }
    
    public void paintText(Graphics g) {
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, (getWidth() - fm.stringWidth(label))/2, (getHeight() + fm.getAscent())/2);
    }
    
    public int getWidthRequest() {
        Graphics g = getGraphics();
        if (g == null) return 1;
        FontMetrics fm = g.getFontMetrics();
        return fm.stringWidth(label) + 10;
    }
    
    public static final int MINIMUM_WIDTH = 80;
    public static final int HEIGHT = 25;
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    
    public Dimension getPreferredSize() {
        int width = Math.max(getWidthRequest(), MINIMUM_WIDTH);
        return new Dimension(width, HEIGHT);
    }
    
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
