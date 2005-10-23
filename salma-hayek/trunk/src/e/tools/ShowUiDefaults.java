package e.tools;

import java.util.*;
import javax.swing.*;

/**
 * Dumps the system's Swing UI defaults to standard output, for your grep(1)
 * pleasure. I'm always wanting to see if there's a UI default I can get a
 * useful default for my own code from, and it's always a pain to trawl
 * through the source (or at least to find the right directory).
 * 
 * In addition, we show the UI defaults for all installed LAFs, to make it
 * easier to check that a default you want to use exists in the other LAFs,
 * or to make it easier to compare values.
 */
public class ShowUiDefaults {
    private ShowUiDefaults() {
    }
    
    private static void showUiDefaults() {
        UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo laf : lafs) {
            showUiDefaultsForLaf(laf);
        }
    }
    
    private static void showUiDefaultsForLaf(UIManager.LookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        
        ArrayList<String> list = new ArrayList<String>();
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        for (Enumeration e = defaults.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            list.add(laf.getName() + ":" + key + "=" + defaults.get(key));
        }
        Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
        for (String line : list) {
            System.out.println(line);
        }
    }
    
    public static void main(String[] args) {
        showUiDefaults();
    }
}
