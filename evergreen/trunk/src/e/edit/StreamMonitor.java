package e.edit;

import e.util.*;
import java.io.*;
import org.jdesktop.swingworker.SwingWorker;

/**
 * Forwards lines of output from the given BufferedReader to the ShellCommand's
 * "process" method. Also informs the ShellCommand when the stream closes.
 */
public class StreamMonitor extends SwingWorker<Object, String> {
    private BufferedReader stream;
    private ShellCommand task;
    
    public StreamMonitor(BufferedReader stream, ShellCommand task) {
        this.stream = stream;
        this.task = task;
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
    protected void process(String... lines) {
        task.process(lines);
    }
}
