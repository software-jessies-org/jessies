package e.edit;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

/**
A simple labelled button. 
*/
public class EButton extends ELabel implements MouseListener {
    /** The action listener. */
    private ActionListener listener;

    /** Whether the button is currently pressed. */
    protected boolean pressed = false;

    /** Whether the button should be considered pressed if the pointer re-enters it. */
    private boolean wasPressed = false;

    public EButton(AbstractAction action) {
        this((String) action.getValue(Action.NAME), action);
    }

    public EButton(String label) {
        this(label, null);
    }

    /** Creates a new button with the given label. */
    public EButton(String label, ActionListener actionListener) {
        super(label);
        addActionListener(actionListener);
        addMouseListener(this);
    }

    /**
     * Draws the button. The button is a thin 3D rectangle lowered when the
     * button is pressed, raised otherwise. The label is drawn in the centre.
     */
    public void paint(Graphics g) {
        paintButtonBorder(g);
        paintText(g);
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
    
    /** Adds an action listener to be notified when the button is clicked. */
    public void addActionListener(ActionListener listener) {
        this.listener = listener;
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
        if (pressed && listener != null) {
            listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getLabel()));
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
}
