package e.util;

import java.io.*;
import java.util.*;

public class ProcessUtilities {
    /**
     * Runs 'command'. Returns the command's status code.
     * Lines written to standard output are appended to 'lines'.
     * Lines written to standard error are appended to 'errors'.
     * FIXME: should errors *we* detect go in 'lines', or in 'errors'?
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
