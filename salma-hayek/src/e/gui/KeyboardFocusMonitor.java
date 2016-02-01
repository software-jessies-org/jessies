package e.gui;

import java.awt.*;
import java.beans.*;

/**
 * Monitors ownership of the keyboard focus.
 * This class saves you from having to have a FocusListener
 * for every instance of a component, or provides a handy way
 * of watching focus changes on other components.
 */
public abstract class KeyboardFocusMonitor implements PropertyChangeListener {
    public KeyboardFocusMonitor() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", this);
    }
    
    public void propertyChange(PropertyChangeEvent e) {
        focusChanged((Component) e.getOldValue(), (Component) e.getNewValue());
    }
    
    public abstract void focusChanged(Component oldOwner, Component newOwner);
}
