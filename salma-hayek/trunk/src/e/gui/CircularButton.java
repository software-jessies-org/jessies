package e.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A circular button. Meant to be used for Dashboard-like buttons to open
 * inspectors, or -- if you subclass and override paintGlyph to draw a cross --
 * cancel buttons like the ones found in AppKit search fields or on closable
 * AppKit tabs.
 */
public class CircularButton extends JComponent {
    private static final Color LIGHT = new Color(0xbababa);
    private static final Color DARK = new Color(0x8b8b8b);
    
    private boolean armed = false;
    
    private ActionListener actionListener = NoOpAction.INSTANCE;
    
    public CircularButton() {
        addMouseListener(new MouseListener() {
            public void mousePressed(MouseEvent e) {
                arm(e);
            }
            public void mouseReleased(MouseEvent e) {
                if (armed) {
                    buttonActivated(e);
                }
                disarm();
            }
            public void mouseClicked(MouseEvent e) {
                // Nothing to do: mouseReleased has already taken care of everything.
            }
            public void mouseExited(MouseEvent e) {
                disarm();
            }
            public void mouseEntered(MouseEvent e) {
                arm(e);
            }
        });
        addMouseMotionListener(new MouseMotionListener() {
            public void mouseMoved(MouseEvent e) {
                // Who cares?
            }
            public void mouseDragged(MouseEvent e) {
                arm(e);
            }
        });
        setFont(new Font("Serif", Font.ITALIC, 16));
    }
    
    private boolean isInterestingEvent(MouseEvent e) {
        return this.contains(e.getPoint()) && SwingUtilities.isLeftMouseButton(e);
    }
    
    private void arm(MouseEvent e) {
        setArmed(isInterestingEvent(e));
    }
    
    private void disarm() {
        setArmed(false);
    }
    
    private void setArmed(boolean newState) {
        armed = newState;
        repaint();
    }
    
    public void paintComponent(Graphics oldGraphics) {
        Graphics2D g = (Graphics2D) oldGraphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintCircle(g);
        paintGlyph(g);
    }
    
    private void paintCircle(Graphics2D g) {
        g.setColor(armed ? DARK : LIGHT);
        final int diameter = 16;
        g.fillOval((getWidth() - diameter) / 2, (getHeight() - diameter) / 2, diameter, diameter);
    }
    
    protected void paintGlyph(Graphics2D g) {
        String label = "i";
        final FontMetrics metrics = g.getFontMetrics();
        final int x = (int) (((double) getWidth() - metrics.getStringBounds(label, g).getWidth()) / 2.0);
        final int y = getHeight() / 2 + metrics.getAscent() / 2;
        g.setColor(Color.WHITE);
        g.drawString(label, x, y);
    }
    
    private void buttonActivated(MouseEvent e) {
        actionListener.actionPerformed(null);
    }
    
    public void setActionListener(ActionListener l) {
        this.actionListener = l;
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(20, 20);
    }
}
