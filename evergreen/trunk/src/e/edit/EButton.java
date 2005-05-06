package e.edit;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import e.gui.*;

/**
 * A simple iconic button for the title bar.
 */
public abstract class EButton extends JComponent implements ActionListener, MouseListener {
    /** Whether the button is currently pressed. */
    protected boolean pressed = false;

    /** Whether the button should be considered pressed if the pointer re-enters it. */
    private boolean wasPressed = false;
    
    public EButton() {
        addMouseListener(this);
    }
    
    public abstract void actionPerformed(ActionEvent e);
    
    /**
     * Draws the button. The button is a thin 3D rectangle lowered when the
     * button is pressed, raised otherwise. The label is drawn in the centre.
     */
    public void paint(Graphics g) {
        paintButtonBorder(g);
    }

    public void paintButtonBorder(Graphics g) {
        int height = getHeight() - 1;
        int width = getWidth() - 1;

        g.setColor(pressed ? new Color(200, 200, 200) : new Color(213, 213, 213));
        g.fillRect(0, 0, width, height);

        g.setColor(pressed ? SystemColor.controlShadow : SystemColor.controlHighlight);
        g.drawLine(0, 0, width, 0);
        g.drawLine(0, 0, 0, height);
        g.setColor(pressed ? SystemColor.controlHighlight : SystemColor.controlShadow);
        g.drawLine(width, 0, width, height);
        g.drawLine(0, height, width, height);
    }

    /** Ignores mouse click events. */
    public void mouseClicked(MouseEvent e) { }

    /** Causes the button to appear pressed when a mouse button is pressed over it. */
    public void mousePressed(MouseEvent e) {
        pressed = true;
        repaint();
    }

    /** Resets the button to its unpressed state, notifying the listener if this constitutes a click. */
    public void mouseReleased(MouseEvent e) {
        if (pressed) {
            actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
        }
        pressed = wasPressed = false;
        repaint();
    }

    /** Handles the case where the mouse leaves the button while a mouse button is down. */
    public void mouseExited(MouseEvent e) {
        wasPressed = pressed;
        pressed = false;
        repaint();
    }

    /** Handles the mouse re-entering the button. */
    public void mouseEntered(MouseEvent e) {
        pressed = wasPressed;
        repaint();
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(20, 20);
    }
}
