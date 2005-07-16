package e.edit;

import e.util.*;
import java.io.*;

public class StreamMonitor extends Thread {
    private BufferedReader stream;
    private ShellCommand task;
    
    public StreamMonitor(BufferedReader stream, ShellCommand task) {
        this.stream = stream;
        this.task = task;
    }
    
    public void run() {
        task.streamOpened();
        try {
            String context = task.getContext();
            String line;
            while ((line = stream.readLine()) != null) {
                task.getWorkspace().reportError(context, line);
            }
        } catch (IOException ex) {
            Log.warn("Unexpected stream closure", ex);
        } finally {
            task.streamClosed();
        }
    }
}
