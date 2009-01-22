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
        KeyStroke keyStroke = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
        initKeyBinding(component, keyStroke, action);
    }
    
    /**
     * Binds an Action to a JComponent via the given KeyStroke.
     */
    public static void initKeyBinding(JComponent component, KeyStroke keyStroke, Action action) {
        String name = (String) action.getValue(Action.NAME);
        component.getActionMap().put(name, action);
        component.getInputMap().put(keyStroke, name);
    }
    
    /**
     * Binds an ActionListener to both double-click and the enter key in the given component.
s     */
    public static void bindDoubleClickAndEnter(final JComponent component, final ActionListener listener) {
        component.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    listener.actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, null, e.getWhen(), e.getModifiers()));
                }
            }
        });
        // The most likely caller hands us an anonymous AbstractAction, so make sure 'action' has a name.
        final Action trampoline = new AbstractAction("e.util.ComponentUtilities.setJListAction") {
            public void actionPerformed(ActionEvent e) {
                listener.actionPerformed(e);
            }
        };
        ComponentUtilities.initKeyBinding(component, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), trampoline);
    }
    
    /**
     * Fixes the page up/down keys and home/end keys to work on another component from the one with the keyboard focus.
     * This lets you offer convenient keyboard navigation like in Apple's Mail, where the arrow keys move through the inbox while the page keys move through the selected message.
     * Note that the home/end keys aren't redirected for text fields, on the assumption that they're needed for cursor movement.
     */
    public static void divertPageScrollingFromTo(final JComponent focusedComponent, final JComponent componentToPageScroll) {
        initKeyBinding(focusedComponent, KeyStroke.getKeyStroke("PAGE_UP"), new AbstractAction("pagePatchUp") {
            public void actionPerformed(ActionEvent e) {
                ComponentUtilities.scroll(componentToPageScroll, true, -1);
            }
        });
        initKeyBinding(focusedComponent, KeyStroke.getKeyStroke("PAGE_DOWN"), new AbstractAction("pagePatchDown") {
            public void actionPerformed(ActionEvent e) {
                ComponentUtilities.scroll(componentToPageScroll, true, 1);
            }
        });
        if (focusedComponent instanceof JTextField == false) {
            initKeyBinding(focusedComponent, KeyStroke.getKeyStroke("HOME"), new AbstractAction("pagePatchToTop") {
                public void actionPerformed(ActionEvent e) {
                    ComponentUtilities.scrollToExtremity(componentToPageScroll, true);
                }
            });
            initKeyBinding(focusedComponent, KeyStroke.getKeyStroke("END"), new AbstractAction("pagePatchToBottom") {
                public void actionPerformed(ActionEvent e) {
                    ComponentUtilities.scrollToExtremity(componentToPageScroll, false);
                }
            });
        }
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
    
    /**
     * Scrolls the given component to its top or bottom.
     * Useful for implementing behavior like in Apple's Mail where home/end in the list cause scrolling in the text.
     */
    public static void scrollToExtremity(JComponent c, boolean top) {
        JScrollBar scrollBar = ((JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, c)).getVerticalScrollBar();
        scrollBar.setValue(top ? scrollBar.getMinimum() : scrollBar.getMaximum());
    }
    
    /**
     * Ensures that all buttons are the same size, and that the chosen size is sufficient to contain the content of any.
     * Most look and feels 
     */
    public static void tieButtonSizes(JButton... buttons) {
        int maxWidth = 0;
        int maxHeight = 0;
        for (JButton button : buttons) {
            Dimension buttonSize = button.getPreferredSize();
            maxWidth = (int) Math.max(buttonSize.getWidth(), maxWidth);
            maxHeight = (int) Math.max(buttonSize.getHeight(), maxHeight);
        }
        Dimension maxButtonSize = new Dimension(maxWidth, maxHeight);
        for (JButton button : buttons) {
            // Seemingly, to get the GTK+ LAF to behave when there are buttons with and without icons, we need to set every size.
            button.setPreferredSize(maxButtonSize);
            button.setMinimumSize(maxButtonSize);
            button.setMaximumSize(maxButtonSize);
            button.setSize(maxButtonSize);
        }
    }
    
    private ComponentUtilities() { /* Not instantiable. */ }
}
