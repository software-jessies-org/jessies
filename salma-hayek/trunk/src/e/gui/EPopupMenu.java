package e.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import e.util.*;

public class EPopupMenu {
    private ArrayList<Action> menuItems = new ArrayList<Action>();
    
    /** Creates a new popup menu. */
    public EPopupMenu() {
    }
    
    /** Adds a single item. */
    public void add(Action item) {
        menuItems.add(item);
    }
    
    /** Adds a separator. */
    public void addSeparator() {
        menuItems.add(null);
    }
    
    /** Adds a group of Action items to the end of the current list of items. */
    public void add(Action[] items) {
        for (int i = 0; i < items.length; i++) {
            menuItems.add(items[i]);
        }
        // Remove any trailing null.
        int lastIndex = menuItems.size() - 1;
        if (menuItems.get(lastIndex) == null) {
            menuItems.remove(lastIndex);
        }
    }
    
    /** Ensures that the menu is on the display & ready for action. */
    public void show(Component origin, int x, int y) {
        if (GuiUtilities.isMacOs()) {
            // Mac OS' JPopupMenu is so bad we should
            // use the AWT PopupMenu instead.
            PopupMenu menu = createMenu(x, y);
            origin.add(menu);
            menu.show(origin, x, y);
        } else {
            JPopupMenu menu = createJMenu(x, y);
            menu.show(origin, x, y);
        }
    }
    
    /** Sets up the menu with items that act upon what's at (x,y). */
    private JPopupMenu createJMenu(final int x, final int y) {
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
    private PopupMenu createMenu(final int x, final int y) {
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
}
