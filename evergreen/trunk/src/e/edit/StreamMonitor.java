package e.edit;

import e.util.*;
import java.io.*;
import java.util.List;
import org.jdesktop.swingworker.SwingWorker;

/**
 * Forwards lines of output from the given BufferedReader to the ShellCommand's
 * "process" method. Also informs the ShellCommand when the stream closes.
 */
public class StreamMonitor extends SwingWorker<Void, String> {
    private BufferedReader stream;
    private ShellCommand task;
    private boolean isStdErr;
    
    public StreamMonitor(BufferedReader stream, ShellCommand task, boolean isStdErr) {
        this.stream = stream;
        this.task = task;
        this.isStdErr = isStdErr;
    }
    
    @Override protected Void doInBackground() throws IOException {
        task.streamOpened();
        String line;
        while ((line = stream.readLine()) != null) {
            publish(line);
        }
        return null;
    }
    
    @Override protected void done() {
        try {
            // Wait for the stream to empty and all lines to have been processed.
            get();
        } catch (Exception ex) {
            Log.warn("Unexpected failure", ex);
        }
        task.streamClosed();
    }
    
    @Override protected void process(List<String> lines) {
        task.processLines(isStdErr, lines);
    }
}
