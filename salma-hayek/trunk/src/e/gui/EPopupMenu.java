package e.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class EPopupMenu {
    private ArrayList menuItems = new ArrayList();
    
    /** Creates a new popup menu. */
    public EPopupMenu() { }
    
    /** Adds a group of EAction items to the end of the current list of items. */
    public void add(Action[] items) {
        for (int i = 0; i < items.length; i++) {
            menuItems.add(items[i]);
        }
    }
    
    /** Ensures that the menu is on the display & ready for action. */
    public void show(Component origin, int x, int y) {
        JPopupMenu menu = createMenu(x, y);
        //origin.add(menu);
        menu.show(origin, x, y);
    }
    
    /** Sets up the menu with items that act upon what's at (x,y). */
    public JPopupMenu createMenu(final int x, final int y) {
        JPopupMenu menu = new JPopupMenu();
        boolean lastWasSeparator = false;
        for (int i = 0; i < menuItems.size(); i++) {
            final Action action = (Action) menuItems.get(i);
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
    
    /** Creates a MenuItem from an EAction and a location. */
    public JMenuItem createMenuItem(final Action action, final int x, final int y) {
        JMenuItem menuItem = new JMenuItem((String) action.getValue(Action.NAME));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                action.actionPerformed(new ActionEvent(new Point(x, y), e.getID(), e.getActionCommand(), e.getModifiers()));
            }
        });
        menuItem.setEnabled(action.isEnabled());
        return menuItem;
    }
}
