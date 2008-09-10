package e.util;

import java.util.*;

/**
 * A utility class to help log timing split throughout a method call.
 * Based on android.util.TimingLogger.
 * 
 * Typical usage is:
 * 
 *     TimingLogger timings = new TimingLogger("methodA");
 *     ... do some work A ...
 *     timings.addSplit("work A");
 *     ... do some work B ...
 *     timings.addSplit("work B");
 *     ... do some work C ...
 *     timings.addSplit("work C");
 *     timings.dumpToLog();
 * 
 * Which would log output something like this:
 * 
 *     methodA: begin
 *     methodA:      9 ms, work A
 *     methodA:      1 ms, work B
 *     methodA:      6 ms, work C
 *     methodA: end, 16 ms
 */
public final class TimingLogger {
    private final String label;
    
    private final long startNanos;
    
    // We're likely to be using TimingLogger in performance-critical code, and unlikely to need many splits.
    private final Split[] splits = new Split[8];
    private int splitCount;
    
    private static class Split {
        private final String splitLabel;
        private final long splitNanos;
        
        private Split(String splitLabel, long splitNanos) {
            this.splitLabel = splitLabel;
            this.splitNanos = splitNanos;
        }
    }
    
    /**
     * Create and initialize a TimingLogger object whose output will be prefixed by the given label.
     */
    public TimingLogger(String label) {
        this.label = label;
        this.startNanos = System.nanoTime();
    }
    
    /**
     * Add a split for the current time, labeled with 'splitLabel'.
     */
    public void addSplit(String splitLabel) {
        splits[splitCount++] = new Split(splitLabel, System.nanoTime());
    }
    
    /**
     * Dumps the timings to the log using Log.warn.
     * If addSplit has never been called, simply output the total time.
     */
    public void dumpToLog() {
        if (splitCount == 0) {
            Log.warn(label + ": took " + TimeUtilities.nsToString(System.nanoTime() - startNanos));
            return;
        }
        Log.warn(label + ": begin");
        long t = startNanos;
        long total = 0;
        for (int i = 0; i < splitCount; ++i) {
            final long splitNanos = splits[i].splitNanos;
            Log.warn(label + ":      " + TimeUtilities.nsToString(splitNanos - t) + ", " + splits[i].splitLabel);
            total += (splitNanos - t);
            t = splitNanos;
        }
        Log.warn(label + ": end, " + TimeUtilities.nsToString(total));
    }
}
