package e.util;

import java.io.*;
import java.util.*;

public class ProcessUtilities {
    /** Returns the lines output to standard output by 'command' when run. */
    public static String[] backQuote(String[] command) {
        ArrayList result = new ArrayList();
        try {
            Process p = Runtime.getRuntime().exec(command);
            p.getOutputStream().close();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.add(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            result.add(ex.getMessage());
        } finally {
            return (String[]) result.toArray(new String[result.size()]);
        }
    }
    
    /** Prevents instantiation. */
    private ProcessUtilities() {
    }
}
