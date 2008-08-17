package e.edit;

import e.util.*;
import java.io.*;
import java.util.List;
import org.jdesktop.swingworker.SwingWorker;

/**
 * Forwards lines of output from the given BufferedReader to the ShellCommand's
 * "process" method. Also informs the ShellCommand when the stream closes.
 */
public class StreamMonitor extends SwingWorker<Object, String> {
    private BufferedReader stream;
    private ShellCommand task;
    private boolean isStdErr;
    
    public StreamMonitor(BufferedReader stream, ShellCommand task, boolean isStdErr) {
        this.stream = stream;
        this.task = task;
        this.isStdErr = isStdErr;
    }
    
    @Override
    protected Object doInBackground() {
        task.streamOpened();
        try {
            String line;
            while ((line = stream.readLine()) != null) {
                publish(line);
            }
        } catch (IOException ex) {
            Log.warn("Unexpected stream closure", ex);
        } finally {
            task.streamClosed();
        }
        return null;
    }
    
    @Override
    protected void process(List<String> lines) {
        task.process(isStdErr, lines);
    }
}
