package e.tools;

import java.util.*;
import javax.swing.*;

/**
 * Dumps the system's Swing UI defaults to standard output, for your grep(1)
 * pleasure. I'm always wanting to see if there's a UI default I can get a
 * useful default for my own code from, and it's always a pain to trawl
 * through the source (or at least to find the right directory). This, if
 * I can just remember its existence, should fix that.
 * FIXME: should showing the values be an option? It makes the output harder
 * to read, and isn't usually what I'm interested in.
 */
public class ShowUiDefaults {
    private ShowUiDefaults() {
    }
    
    public static void main(String[] args) {
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        for (Enumeration e = defaults.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            System.out.println(key + "=" + defaults.get(key));
        }
    }
}
