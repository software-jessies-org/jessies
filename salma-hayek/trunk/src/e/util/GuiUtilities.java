package e.util;

import javax.swing.*;

public class GuiUtilities {
    private GuiUtilities() { /* Not instantiable. */ }
    
    /**
     * Tests whether we're running on Mac OS. The Mac is quite
     * different from Linux and Windows, and it's sometimes
     * necessary to put in special-case behavior if you're running
     * on the Mac.
     */
    public static boolean isMacOs() {
        return (System.getProperty("os.name").indexOf("Mac") != -1);
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
