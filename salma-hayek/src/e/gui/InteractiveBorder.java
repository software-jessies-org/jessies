package e.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

/**
 * A border that takes care of button-like behavior. Simply override
 * buttonActivated to supply your own behavior. Use isArmed in paintBorder
 * to change your appearance based on whether or not the button is "depressed".
 */
public abstract class InteractiveBorder extends EmptyBorder implements MouseInputListener {
    private boolean activateOnPress = false;
    private boolean armed = false;
    private boolean onLeft;
    private int sideLength;
    private JComponent component;
    
    /**
     * We assume that the addition to the border is square, and can only go
     * on the left or right.
     */
    InteractiveBorder(int sideLength, boolean onLeft) {
        super(makeInsetsForPosition(sideLength, onLeft));
        this.sideLength = sideLength;
        this.onLeft = onLeft;
    }
    
    /**
     * Sets whether you want menu-like behavior (true) or button-like
     * behavior (false). The default is false, and buttonActivated is
     * only invoked after a press and release, both on-button.
     */
    public void setActivateOnPress(boolean newState) {
        this.activateOnPress = newState;
    }
    
    /**
     * Attaches this border to the given component, using a CompoundBorder.
     * You can add both a left and a right border to a component by invoking
     * this once on each border with the same component argument.
     */
    public void attachTo(JComponent c) {
        this.component = c;
        c.setBorder(BorderFactory.createCompoundBorder(c.getBorder(),  this));
        c.addMouseListener(this);
        c.addMouseMotionListener(this);
    }
    
    /**
     * Invoked when the button is activated; what 'activated' means depends
     * on whether or not this border is set to activate on press.
     */
    public abstract void buttonActivated(MouseEvent e);
    
    /**
     * Tests whether the 'button' this border represents is armed. Armed means
     * that the user has pressed the mouse button over us, but not yet
     * released the mouse button.
     * 
     * You can call this from paintBorder to change your appearance (buttons
     * tend to depress or look darker, for example, when armed).
     */
    public boolean isArmed() {
        return armed;
    }
    
    private static Insets makeInsetsForPosition(int sideLength, boolean left) {
        if (left) {
            return new Insets(0, sideLength, 0, 0);
        } else {
            return new Insets(0, 0, 0, sideLength);
        }
    }
    
    private boolean isOverButton(MouseEvent e) {
        // If the button is down, we might be outside the component
        // without having had mouseExited invoked.
        if (component.contains(e.getPoint()) == false) {
            return false;
        }
        
        // If the mouse wasn't in the border, it can't be over a button.
        Rectangle interior = SwingUtilities.calculateInnerArea(component, null);
        if (interior.contains(e.getPoint())) {
            return false;
        }
        
        if (onLeft && e.getX() < sideLength) {
            return true;
        } else if (!onLeft && e.getX() > component.getWidth() - sideLength) {
            return true;
        }
        return false;
    }
    
    public void mouseDragged(MouseEvent e) {
        arm(e);
    }
    
    public void mouseEntered(MouseEvent e) {
        arm(e);
    }
    
    public void mouseExited(MouseEvent e) {
        disarm();
    }
    
    public void mousePressed(MouseEvent e) {
        if (activateOnPress && isInterestingEvent(e)) {
            buttonActivated(e);
        }
        arm(e);
    }
    
    public void mouseReleased(MouseEvent e) {
        if (armed) {
            buttonActivated(e);
        }
        disarm();
    }
    
    public void mouseMoved(MouseEvent e) {
        // Who cares?
    }
    
    public void mouseClicked(MouseEvent e) {
        // Nothing to do: mouseReleased has already taken care of everything.
    }
    
    private boolean isInterestingEvent(MouseEvent e) {
        return (isOverButton(e) && SwingUtilities.isLeftMouseButton(e));
    }
    
    private void arm(MouseEvent e) {
        setArmed(isInterestingEvent(e));
    }
    
    private void disarm() {
        setArmed(false);
    }
    
    private void setArmed(boolean newState) {
        if (activateOnPress) {
            // If we activate on a button press, there can be no notion of
            // being armed.
            return;
        }
        armed = newState;
        component.repaint();
    }
}
