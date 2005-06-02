package e.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import e.util.*;

/**
 * Attaches a pop-up menu to a Component. The menu is built by querying a
 * collection of MenuItemProvider instances for menu items each time the menu
 * needs to be shown.
 */
public class EPopupMenu {
    private ArrayList<MenuItemProvider> providers = new ArrayList<MenuItemProvider>();
    private Component componentWithMenu;
    
    private void attachPopupMenuTo(Component c) {
        componentWithMenu = c;
        componentWithMenu.addMouseListener(new MenuMouseListener());
    }
    
    /** Creates a new popup menu. */
    public EPopupMenu(Component component) {
        attachPopupMenuTo(component);
    }
    
    public void addMenuItemProvider(MenuItemProvider provider) {
        providers.add(provider);
    }
    
    public void removeMenuItemProvider(MenuItemProvider provider) {
        providers.remove(provider);
    }
    
    /** Ensures that the menu is on the display & ready for action. */
    private void show(List<Action> menuItems, Component origin, int x, int y) {
        if (GuiUtilities.isMacOs()) {
            // Mac OS' JPopupMenu is so bad we should
            // use the AWT PopupMenu instead.
            PopupMenu menu = createMenu(menuItems, x, y);
            origin.add(menu);
            menu.show(origin, x, y);
        } else {
            JPopupMenu menu = createJMenu(menuItems, x, y);
            menu.show(origin, x, y);
        }
    }
    
    /** Sets up the menu with items that act upon what's at (x,y). */
    private JPopupMenu createJMenu(List<Action> menuItems, final int x, final int y) {
        JPopupMenu menu = new JPopupMenu();
        boolean lastWasSeparator = false;
        for (Action action : menuItems) {
            if (action == null) {
                if (lastWasSeparator == false) {
                    menu.addSeparator();
                }
                lastWasSeparator = true;
            } else {
                JMenuItem menuItem = new JMenuItem(action);
                menuItem.setAccelerator((KeyStroke) action.getValue(Action.ACCELERATOR_KEY));
                menu.add(menuItem);
                lastWasSeparator = false;
            }
        }
        return menu;
    }
    
    /** Sets up the menu with items that act upon what's at (x,y). */
    private PopupMenu createMenu(List<Action> menuItems, final int x, final int y) {
        PopupMenu menu = new PopupMenu();
        boolean lastWasSeparator = false;
        for (Action action : menuItems) {
            if (action == null) {
                if (lastWasSeparator == false) {
                    menu.addSeparator();
                }
                lastWasSeparator = true;
            } else {
                menu.add(createMenuItem(action, x, y));
                lastWasSeparator = false;
            }
        }
        return menu;
    }
    
    /** Creates a MenuItem from an Action and a location. */
    private MenuItem createMenuItem(final Action action, final int x, final int y) {
        MenuItem menuItem = new MenuItem((String) action.getValue(Action.NAME));
        KeyStroke key = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
        if (key != null) {
            menuItem.setShortcut(new MenuShortcut(key.getKeyCode(), (key.getModifiers() & InputEvent.SHIFT_MASK) != 0));
        }
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                action.actionPerformed(new ActionEvent(new Point(x, y), e.getID(), e.getActionCommand(), e.getModifiers()));
            }
        });
        menuItem.setEnabled(action.isEnabled());
        return menuItem;
    }
    
    public class MenuMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            handle(e);
        }
        
        public void mouseReleased(MouseEvent e) {
            handle(e);
        }
        
        private void handle(MouseEvent e) {
            if (e.isConsumed() == false && e.isPopupTrigger()) {
                requestFocusOnComponentWithMenu();
                showPopupMenuLater(e);
            }
        }
    }
    
    private void requestFocusOnComponentWithMenu() {
        componentWithMenu.requestFocus();
    }
    
    private void showPopupMenuLater(final MouseEvent e) {
        try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    showPopupMenu(e);
                }
            });
        } catch (Exception ex) {
            Log.warn("Exception while trying to show pop-up menu", ex);
        }
    }
    
    private void showPopupMenu(MouseEvent e) {
        List<Action> menuItems = new ArrayList<Action>();
        
        for (MenuItemProvider provider : providers) {
            // Collect this provider's items.
            List<Action> newItems = new ArrayList<Action>();
            provider.provideMenuItems(e, newItems);
            
            // Did it have anything to add?
            if (newItems.size() > 0) {
                if (menuItems.size() > 0) {
                    // Make sure we've got a separator between the last
                    // provider's items and the new items.
                    menuItems.add(null);
                }
                menuItems.addAll(newItems);
            }
        }
        
        // Do we have anything worth showing?
        if (menuItems.size() > 0) {
            show(menuItems, (Component) e.getSource(), e.getX(), e.getY());
        }
    }
}
