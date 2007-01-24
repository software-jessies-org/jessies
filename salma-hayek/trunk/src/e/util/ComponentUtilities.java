package e.util;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class ComponentUtilities {
    /**
     * Disables the forward and backward focus traversal keys on the given
     * component.
     */
    public static void disableFocusTraversal(Component c) {
        Set<AWTKeyStroke> emptySet = Collections.emptySet();
        c.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, emptySet);
        c.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, emptySet);
    }
    
    /**
     * Binds an Action to a JComponent via the Action's configured ACCELERATOR_KEY.
     */
    public static void initKeyBinding(JComponent component, Action action) {
        String name = (String) action.getValue(Action.NAME);
        KeyStroke keyStroke = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
        component.getActionMap().put(name, action);
        component.getInputMap().put(keyStroke, name);
    }
    
    /**
     * Fixes the page up/down to work on another component from the one with the keyboard focus.
     * This lets you offer convenient keyboard navigation like in Apple's Mail, where the arrow keys move through the inbox while the page keys move through the selected message.
     */
    public static void divertPageScrollingFromTo(final JComponent focusedComponent, final JComponent componentToPageScroll) {
        focusedComponent.getInputMap().put(KeyStroke.getKeyStroke("PAGE_UP"), "pagePatchUp");
        focusedComponent.getInputMap().put(KeyStroke.getKeyStroke("PAGE_DOWN"), "pagePatchDown");
        focusedComponent.getActionMap().put("pagePatchUp", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ComponentUtilities.scroll(componentToPageScroll, true, -1);
            }
        });
        focusedComponent.getActionMap().put("pagePatchDown", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ComponentUtilities.scroll(componentToPageScroll, true, 1);
            }
        });
    }
    
    /**
     * Scrolls the given component by its unit or block increment in the given direction (actually a scale factor, so use +1 or -1).
     * Useful for implementing behavior like in Apple's Mail where page up/page down in the list cause scrolling in the text.
     */
    public static void scroll(JComponent c, boolean byBlock, int direction) {
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, c);
        JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
        int increment = byBlock ? scrollBar.getBlockIncrement(direction) : scrollBar.getUnitIncrement(direction);
        int newValue = scrollBar.getValue() + direction * increment;
        newValue = Math.min(newValue, scrollBar.getMaximum());
        newValue = Math.max(newValue, scrollBar.getMinimum());
        scrollBar.setValue(newValue);
    }
    
    private ComponentUtilities() { /* Not instantiable. */ }
}
