package e.util;

import java.awt.*;
import javax.swing.*;

public class GuiUtilities {
    private GuiUtilities() { /* Not instantiable. */ }

    /**
     * The background color for alternate rows in lists and tables.
     */
    public static final Color ALTERNATE_ROW_COLOR = new Color(0.92f, 0.95f, 0.99f);
    
    /**
     * The background color for selected rows in lists and tables.
     */
    public static final Color SELECTED_ROW_COLOR = new Color(0.24f, 0.50f, 0.87f);
    
    static {
        UIManager.put("List.selectionBackground", SELECTED_ROW_COLOR);
        UIManager.put("List.selectionForeground", Color.WHITE);
        UIManager.put("Table.selectionBackground", SELECTED_ROW_COLOR);
        UIManager.put("Table.selectionForeground", Color.WHITE);
    }
    
    /**
     * Tests whether we're running on Mac OS. The Mac is quite
     * different from Linux and Windows, and it's sometimes
     * necessary to put in special-case behavior if you're running
     * on the Mac.
     */
    public static boolean isMacOs() {
        return (System.getProperty("os.name").indexOf("Mac") != -1);
    }
    
    /**
     * Tests whether we're running on Windows.
     */
    public static boolean isWindows() {
        return (System.getProperty("os.name").indexOf("Windows") != -1);
    }
    
    public static void initLookAndFeel() {
        try {
            String lafClassName = Parameters.getParameter("laf.className");
            if (lafClassName == null) {
                lafClassName = UIManager.getSystemLookAndFeelClassName();
            }
            if (lafClassName.indexOf("GTK") != -1) {
                lafClassName = UIManager.getCrossPlatformLookAndFeelClassName();
            }
            UIManager.setLookAndFeel(lafClassName);
            if (lafClassName.indexOf("Metal") != -1) {
                Object font = UIManager.get("Table.font");
                UIManager.put("Menu.font", font);
                UIManager.put("MenuItem.font", font);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
