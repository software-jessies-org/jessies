package e.gui;

import e.util.*;
import javax.swing.*;

public class GnomeStockIcon {
    /**
     * These are type-safe versions of the hard-coded numbers in GTKStyle, for
     * use with getGnomeStockIcon.
     */
    public enum Size {
        GTK_ICON_SIZE_INVALID,
        GTK_ICON_SIZE_MENU, // 16 x 16
        GTK_ICON_SIZE_SMALL_TOOLBAR, // 18x18
        GTK_ICON_SIZE_LARGE_TOOLBAR, // 24x24
        GTK_ICON_SIZE_BUTTON, // 20x20
        GTK_ICON_SIZE_DND, // 32x32
        GTK_ICON_SIZE_DIALOG // 48x48
    }
    
    private GnomeStockIcon() {
    }
    
    /**
     * Sets the SMALL_ICON property to the given GNOME stock icon, assuming
     * it's available. The SMALL_ICON property is left unchanged otherwise.
     */
    public static void useStockIcon(AbstractAction action, String name) {
        // FIXME: for Java 6, use Action.LARGE_ICON_KEY too.
        Icon icon = getStockIcon(name, Size.GTK_ICON_SIZE_MENU);
        if (icon != null) {
            action.putValue(AbstractAction.SMALL_ICON, icon);
        }
    }
    
    /**
     * Sets the button's icon to the given GNOME stock icon, assuming
     * it's available.
     */
    public static void useStockIcon(JButton button, String name) {
        Icon icon = getStockIcon(name, Size.GTK_ICON_SIZE_BUTTON);
        if (icon != null) {
            button.setIcon(icon);
        }
    }
    
    /**
     * Returns an Icon for one of the GNOME stock icons. If the icon is not
     * available for any reason, you'll get null. (Not using the GTK LAF is
     * one reason why.)
     * The GNOME header file listing the possible strings is here:
     * http://cvs.gnome.org/viewcvs/gtk%2B/gtk/gtkstock.h?view=markup
     */
    public static Icon getStockIcon(String name, Size size) {
        if (GuiUtilities.isGtk() == false) {
            return null;
        }
        
        Icon icon = null;
        try {
            Class<?> gtkStockIconClass = Class.forName("com.sun.java.swing.plaf.gtk.GTKStyle$GTKStockIcon");
            java.lang.reflect.Constructor constructor = gtkStockIconClass.getDeclaredConstructor(String.class, int.class);
            constructor.setAccessible(true);
            icon = (Icon) constructor.newInstance(name, size.ordinal());
        } catch (Exception ex) {
            // Sorry! No icon for you!
        }
        return icon;
    }
}
