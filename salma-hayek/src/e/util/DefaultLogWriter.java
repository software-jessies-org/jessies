package e.util;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Respects e.util.Log.filename property if set, otherwise uses stderr.
 * 
 * @author mth
 */
class DefaultLogWriter implements LogWriter {

    private final String applicationName;
    private PrintWriter out = new PrintWriter(System.err, true);

    public DefaultLogWriter(final String applicationName) {
        this.applicationName = applicationName;
        // We take care to initialize 'out' first so we can log if this goes wrong. 
        String logFilename = System.getProperty("e.util.Log.filename");
        try {
            if (logFilename != null) {
                // Append to, rather than truncate, the log.
                FileOutputStream fileOutputStream = new FileOutputStream(logFilename, true);
                // Use the UTF-8 character encoding.
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
                // Auto-flush when println is called.
                out = new PrintWriter(outputStreamWriter, true);
            }
        } catch (Throwable th) {
            log("Couldn't redirect logging to \"" + logFilename + "\"", th);
        }
    }

    public void log(final String message, final Throwable th) {
        out.println(TimeUtilities.currentIsoString() + " " + applicationName + ": " + message);
        if (th != null) {
            out.println("Associated exception:");
            th.printStackTrace(out);
        }
    }

}
