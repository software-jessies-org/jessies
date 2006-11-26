package e.gui;

import e.util.*;
import java.awt.*;
import java.awt.image.*;
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
            workAroundLafBugs(button);
        }
    }
    
    private static void workAroundLafBugs(JButton button) {
        // FIXME: there seems to be a bug with the GTK LAF that means when the button is disabled, we see the icon and "...", and only when it's enabled do we see "Commit". This is a work-around.
        // Note that the code below for the disabled icon relies on this (the call to getPreferredSize forces the image to be loaded).
        button.setPreferredSize(button.getPreferredSize());
        
        // FIXME: Java 6 looks like it will ship with a bug that means disabled icons aren't right for their theme.
        // This workaround is close enough (but not exact) for the Ubuntu 6.10 "Human" theme.
        Icon icon = button.getIcon();
        Image image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        RGBImageFilter filter = new RGBImageFilter() {
            // Based on the comment from "jansan" on http://weblogs.java.net/blog/joshy/archive/2006/08/windows_lf_bugs.html but with a different alpha scale factor to better match Ubuntu 6.10's "Human" theme.
            public int filterRGB(int x, int y, int rgb) {
                int red = (rgb >> 16) & 0xff;
                int green = (rgb >> 8) & 0xff;
                int blue = rgb & 0xff;
                int average = (red + green + blue) / 3;
                int alpha = (rgb >> 24) & 0xff;
                int newAlpha = (int) (alpha * 0.2);
                int newRgb = (newAlpha << 24) | (average << 16) | (average << 8) | average;
                return newRgb;
            }
        };
        Image grayImage = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(image.getSource(), filter));
        button.setDisabledIcon(new ImageIcon(grayImage));
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
