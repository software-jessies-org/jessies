package e.gui;

import e.util.*;
import java.awt.*;
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
        boolean haveLocalBinding = super.processKeyBinding(ks, e, condition, pressed);
        if (haveLocalBinding) {
            return true;
        }
        MenuElement[] subElements = getSubElements();
        for (MenuElement subElement : getSubElements()) {
            if (processBindingForKeyStrokeRecursive(subElement, ks, e, condition, pressed)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean processBindingForKeyStrokeRecursive(MenuElement element, KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        if (element == null) {
            return false;
        }
        
        Component c = element.getComponent();
        if (c != null && c instanceof JMenuItem && processJMenuItemKeyBinding((JMenuItem) c, ks, e, condition, pressed)) {
            return true;
        }
        
        for(MenuElement subElement : element.getSubElements()) {
            if (processBindingForKeyStrokeRecursive(subElement, ks, e, condition, pressed)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean processJMenuItemKeyBinding(JMenuItem item, KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        InputMap map = item.getInputMap(condition);
        ActionMap am = item.getActionMap();
        if (map == null || am == null) {
            return false;
        }
        Object binding = map.get(ks);
        if (binding == null) {
            return false;
        }
        Action action = am.get(binding);
        // This is the key change: we check Action.isEnabled rather than JMenuItem.isEnabled.
        if (action == null || action.isEnabled() == false) {
            return false;
        }
        return SwingUtilities.notifyAction(action, ks, e, item, e.getModifiers());
    }
    
    private static class MenuItemStateUpdater implements MenuListener {
        private static final MenuListener INSTANCE = new MenuItemStateUpdater();
        
        public void menuCanceled(MenuEvent e) {
        }
        
        public void menuSelected(MenuEvent e) {
            traverseMenu(JMenu.class.cast(e.getSource()));
        }
        
        public void menuDeselected(MenuEvent e) {
        }
        
        private void traverseMenu(JMenu menu) {
            MenuElement[] elements = menu.getSubElements();
            for (int i = 0; i < elements.length; i++) {
                if (elements[i] instanceof JMenu) {
                    traverseMenu((JMenu) elements[i]);
                } else if (elements[i] instanceof JPopupMenu) {
                    traverseMenu((JPopupMenu) elements[i]);
                } else {
                    JMenuItem menuItem = (JMenuItem) elements[i];
                    Action action = menuItem.getAction();
                    if (action == null) {
                        if (menuItem instanceof JMenu == false) {
                            Log.warn("Actionless menu item found: " + menuItem);
                        }
                    } else {
                        menuItem.setEnabled(action.isEnabled());
                    }
                }
            }
        }
        
        private void traverseMenu(JPopupMenu menu) {
            MenuElement[] elements = menu.getSubElements();
            for (int i = 0; i < elements.length; i++) {
                JMenuItem menuItem = (JMenuItem) elements[i];
                Action action = menuItem.getAction();
                if (action == null) {
                    if (menuItem instanceof JMenu == false) {
                        Log.warn("Actionless popup menu item found: " + menuItem);
                    }
                } else {
                    menuItem.setEnabled(action.isEnabled());
                }
            }
        }
    }
}
