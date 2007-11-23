package e.gui;

import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;

public class GnomeStockIcon {
    /**
     * These are type-safe versions of the hard-coded numbers in GTKStyle, for
     * use with getStockIcon.
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
    
    // Java 6: this is in Action in Java 6.
    private static final String LARGE_ICON_KEY = "SwingLargeIconKey";
    
    private static HashMap<String, String> nameMap;
    
    private GnomeStockIcon() {
    }
    
    /**
     * Sets the SMALL_ICON property to the given GNOME stock icon, assuming
     * it's available. The SMALL_ICON property is left unchanged otherwise.
     */
    public static void useStockIcon(Action action, String name) {
        Icon menuIcon = getStockIcon(name, Size.GTK_ICON_SIZE_MENU);
        if (menuIcon != null) {
            action.putValue(Action.SMALL_ICON, menuIcon);
        }
        Icon buttonIcon = getStockIcon(name, Size.GTK_ICON_SIZE_BUTTON);
        if (buttonIcon != null) {
            action.putValue(/*Action.*/LARGE_ICON_KEY, buttonIcon);
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
    
    private static String getGtkIconNameForString(String name) {
        initNameMap();
        return nameMap.get(name);
    }
    
    private static synchronized void initNameMap() {
        if (nameMap == null) {
            nameMap = new HashMap<String, String>();
            // FIXME: explicitly listing those choices which can have "..." isn't necessarily the right choice, but it does mean we can insist that, say, "Save As..." always does but "Paste" never does.
            nameMap.put("Add", "gtk-add");
            nameMap.put("Add...", "gtk-add");
            nameMap.put("Apply", "gtk-apply");
            nameMap.put("Cancel", "gtk-cancel");
            nameMap.put("Close", "gtk-close");
            nameMap.put("Copy", "gtk-copy");
            nameMap.put("Cut", "gtk-cut");
            nameMap.put("Credits", "gtk-about");
            nameMap.put("Delete", "gtk-delete");
            nameMap.put("Edit...", "gtk-edit");
            nameMap.put("Find...", "gtk-find");
            nameMap.put("Go to Line...", "gtk-jump-to");
            nameMap.put("Help", "gtk-help");
            nameMap.put("OK", "gtk-ok");
            nameMap.put("Open", "gtk-open");
            nameMap.put("Paste", "gtk-paste");
            nameMap.put("Preferences...", "gtk-preferences");
            nameMap.put("Quit", "gtk-quit");
            nameMap.put("Redo", "gtk-redo");
            nameMap.put("Refresh", "gtk-refresh");
            nameMap.put("Remove", "gtk-remove");
            nameMap.put("Replace", "gtk-find-and-replace");
            nameMap.put("Rescan", "gtk-refresh");
            nameMap.put("Revert", "gtk-revert-to-saved");
            nameMap.put("Revert to Saved", "gtk-revert-to-saved");
            nameMap.put("Run", "gtk-execute");
            nameMap.put("Undo", "gtk-undo");
            nameMap.put("Save", "gtk-save");
            nameMap.put("Save As...", "gtk-save-as");
            nameMap.put("Select All", "gtk-select-all");
            nameMap.put("Show Info", "gtk-info");
        }
    }
    
    public static void configureButton(JButton button) {
        if (GuiUtilities.isGtk() == false) {
            return;
        }
        String buttonText = button.getText();
        String gtkIconName = getGtkIconNameForString(buttonText);
        if (gtkIconName != null) {
            useStockIcon(button, gtkIconName);
            // FIXME: we should probably support more mnemonics, and factor this out into another map.
            if (buttonText.equals("Cancel") || buttonText.equals("Close")) {
                button.setMnemonic(KeyEvent.VK_C);
            } else if (buttonText.equals("OK")) {
                button.setMnemonic(KeyEvent.VK_O);
            }
        }
    }
    
    public static void configureAction(Action action) {
        if (GuiUtilities.isGtk() == false) {
            return;
        }
        String actionName = (String) action.getValue(Action.NAME);
        String gtkIconName = getGtkIconNameForString(actionName);
        if (gtkIconName != null) {
            useStockIcon(action, gtkIconName);
            // FIXME: we should perhaps support mnemonics; beware that configureAction mainly affects menus, so mnemonics may be meaningless/wrong.
        }
    }
    
    // Java 6 shipped with a bug that means disabled icons aren't right for their theme.
    // FIXME: work out how to set the disabled icon on an Action, so we can fix our disabled menu items' icons.
    private static void workAroundLafBugs(JButton button) {
        // FIXME: there seems to be a bug with the GTK LAF that means when the button is disabled, we see the icon and "...", and only when it's enabled do we see "Commit". This is a work-around.
        // Note that the code below for the disabled icon relies on this (the call to getPreferredSize forces the image to be loaded).
        button.setPreferredSize(button.getPreferredSize());
        
        // This workaround seems close enough (but not exact) for the Ubuntu 6.10 "Human" theme.
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
     * Returns an Icon for one of the GNOME stock icons.
     * If the icon is not available for any reason, you'll get null.
     * (Not using the GTK LAF is one reason why.)
     * The GNOME header file listing the possible strings is here:
     * http://cvs.gnome.org/viewcvs/gtk%2B/gtk/gtkstock.h?view=markup
     */
    public static Icon getStockIcon(String name, Size size) {
        if (GuiUtilities.isGtk() == false) {
            return null;
        }
        
        String lazyDesktopPropertyName = "gtk.icon." + name + '.' + size.ordinal() + '.' + "ltr";
        Image image = (Image) Toolkit.getDefaultToolkit().getDesktopProperty(lazyDesktopPropertyName);
        if (image != null) {
            return new ImageIcon(image);
        }
        
        return null;
    }
}
