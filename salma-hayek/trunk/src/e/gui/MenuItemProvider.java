package e.gui;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Provides menu items to appear on an EPopupMenu.
 */
public interface MenuItemProvider {
    /**
     * Invoked to add Action instances to be used as menu items in response to
     * the given MouseEvent. A null value can be added to the collection to
     * request a separator appear at that position. A provider should not
     * attempt to add separators at the beginning or end of its group of
     * actions: EPopupMenu will take care of that.
     */
    public void provideMenuItems(MouseEvent event, Collection<Action> actions);
}
