package e.gui;

import e.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Makes it easier to enable/disable an Action on a JMenuBar. Swing uses a
 * push model for enabling/disabling Actions. This is great if you have a menu
 * and a toolbar, because it means you get both updated. But it means that you
 * have to pay for the complicated case (by putting setEnabled all over your
 * code) even if you only want the simple case: you can't just override
 * Action.isEnabled because it won't be checked every time the menu is shown.
 * 
 * As well as not working with toolbars, the other problem with this code is
 * that there's no way to disable a top-level menu (that is, a JMenu added
 * directly to the JMenuBar) because it works by listening for the menu to be
 * selected, and goes through each of the menu's JMenuItem's Actions, invoking
 * isEnabled and using the results with setEnabled on the JMenuItems
 * themselves. Top-level menus are always visible, so we have no chance to
 * fix them up. This is the same as the problem with toolbars, which are also
 * always visible.
 * 
 * So although you can't win, you can choose between a simple solution that
 * gives relatively stand-alone Action classes (but doesn't work for toolbars),
 * or the normal Swing solution that requires the knowledge about when actions
 * should be enabled/disabled to leak outside the actions themselves (but does
 * work for toolbars).
 */
public class EMenuBar extends JMenuBar {
    /**
     * Overrides add to listen for "menu" being selected so we can update its
     * enabled/disabled state.
     */
    @Override
    public JMenu add(JMenu menu) {
        menu.addMenuListener(MenuItemStateUpdater.INSTANCE);
        return super.add(menu);
    }
    
    /**
     * Overridden here to check whether the Action (rather than the JMenuItem) is enabled.
     * Overridden in JMenuBar to check all the child menus.
     */
    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        updateMenuItemEnabledStates(this);
        return super.processKeyBinding(ks, e, condition, pressed);
    }
    
    /**
     * Traverses a menu recursively, setting each menu item's enabled state from the item's action's state.
     * This way, the application need only update the actions' states, and the menu bar just does the right thing.
     */
    private static void updateMenuItemEnabledStates(MenuElement menu) {
        for (MenuElement element : menu.getSubElements()) {
            if (element instanceof JMenu) {
                updateMenuItemEnabledStates((JMenu) element);
            } else if (element instanceof JPopupMenu) {
                updateMenuItemEnabledStates((JPopupMenu) element);
            } else {
                JMenuItem menuItem = (JMenuItem) element;
                Action action = menuItem.getAction();
                if (action == null) {
                    Log.warn("Actionless menu item found: " + menuItem);
                } else {
                    menuItem.setEnabled(action.isEnabled());
                }
            }
        }
    }
    
    private static class MenuItemStateUpdater implements MenuListener {
        private static final MenuListener INSTANCE = new MenuItemStateUpdater();
        
        public void menuCanceled(MenuEvent e) {
        }
        
        public void menuSelected(MenuEvent e) {
            updateMenuItemEnabledStates((JMenu) e.getSource());
        }
        
        public void menuDeselected(MenuEvent e) {
        }
    }
    
    /**
     * This isn't a JMenuBar facility - it's used by Evergreen's Minibuffer to forward key strokes.
     */
    public void performActionForKeyStroke(KeyStroke keyStroke, KeyEvent e) {
        updateMenuItemEnabledStates(this);
        processKeyBinding(keyStroke, e, JComponent.WHEN_IN_FOCUSED_WINDOW, e.getID() == KeyEvent.KEY_PRESSED);
    }
}
