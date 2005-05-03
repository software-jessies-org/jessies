package e.tools;

import java.util.*;

/**
 * Dumps the system's properties to standard output, for your grep(1)
 * pleasure.
 * FIXME: should showing the values be an option? It makes the output harder
 * to read, and isn't usually what I'm interested in.
 * FIXME: factor out the commonality between this and ShowUiDefaults.
 */
public class ShowProperties {
    private ShowProperties() {
    }
    
    public static void main(String[] args) {
        Properties properties = System.getProperties();
        for (Enumeration e = properties.keys() ; e.hasMoreElements() ;) {
            String key = (String) e.nextElement();
            System.out.println(key + "=" + properties.get(key));
        }
    }
}
