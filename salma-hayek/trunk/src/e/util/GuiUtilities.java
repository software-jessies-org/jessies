package e.util;

import javax.swing.*;

public class GuiUtilities {
    private GuiUtilities() { /* Not instantiable. */ }
    
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
