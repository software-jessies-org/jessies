package e.util;

import java.io.*;
import java.util.*;

public class ProcessUtilities {
    /**
     * Runs 'command'. Returns the command's status code.
     * Lines written to standard output are appended to 'lines'.
     * Lines written to standard error are appended to 'errors'.
     * 
     * FIXME: should errors *we* detect go in 'lines', or in 'errors'? Currently
     * they go in 'lines'.
     * 
     * If directory is null, the subprocess inherits our working directory.
     * 
     * You can use the same ArrayList for 'lines' and 'errors'. All the error
     * lines will appear after all the output lines.
     */
    public static int backQuote(File directory, String[] command, ArrayList lines, ArrayList errors) {
        ArrayList result = new ArrayList();
        try {
            Process p = Runtime.getRuntime().exec(command, null, directory);
            p.getOutputStream().close();
            readLinesFromStream(lines, p.getInputStream());
            readLinesFromStream(errors, p.getErrorStream());
            return p.waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
            lines.add(ex.getMessage());
            return 1;
        }
    }

    /**
     * Runs a command and ignores the output. The child is waited for on a
     * separate thread, so there's no indication of whether the spawning was
     * successful or not. A better design might be to exec in the current
     * thread, and hand the Process over to another Thread; you'd still not
     * get the return code (but losing that is part of the deal), but you
     * would at least know that Java had no trouble in exec. Is that worth
     * anything?
     */
    public static void spawn(final File directory, final String[] command) {
        new Thread() {
            public void run() {
                try {
                    Process p = Runtime.getRuntime().exec(command, null, directory);
                    p.getOutputStream().close();
                    p.waitFor();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.start();
    }

    private static void readLinesFromStream(ArrayList result, InputStream stream) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = in.readLine()) != null) {
                result.add(line);
            }
            in.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            result.add(ex.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    result.add(ex.getMessage());
                }
            }
        }
    }
    
    /** Prevents instantiation. */
    private ProcessUtilities() {
    }
}
