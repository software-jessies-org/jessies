package e.util;

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
     * 
     * FIXME: we might want a variant that takes a lower bound (in seconds, as a double), and only logs if the total time was greater than that.
     * FIXME: the output for multiple splits is a bit ugly; ideally, the times would be right-aligned.
     * FIXME: is the "begin" line useful?
     * FIXME: it's unfortunate for this caller that each log line has a timestamp (because it won't correspond to the times of the actions).
     */
    public void dumpToLog() {
        if (splitCount == 0) {
            Log.warn(label + ": took " + nsToString(System.nanoTime() - startNanos) + ".");
        } else if (splitCount == 1) {
            Log.warn(label + ": " + splits[0].splitLabel + " in " + nsToString(splits[0].splitNanos - startNanos) + ".");
        } else {
            Log.warn(label + ": begin");
            long t = startNanos;
            long total = 0;
            for (int i = 0; i < splitCount; ++i) {
                final long splitNanos = splits[i].splitNanos;
                Log.warn(label + ":      " + nsToString(splitNanos - t) + ", " + splits[i].splitLabel);
                total += (splitNanos - t);
                t = splitNanos;
            }
            Log.warn(label + ": end, " + nsToString(total));
        }
    }
    
    // We don't use TimeUtilities.nsToString because that tries to be clever and convert to human-readable units.
    // The problem with that for this application is that we really want to make it easy to *compare* times.
    // That's made a lot easier when all times are shown in the same unit (here seconds).
    // TimeUtilities.nsToString makes it too easy to think 30 us is more important than 7 ms.
    private static String nsToString(long ns) {
        // FIXME: if we're looking at things that take more than a second, we might want to specify a 'width' too.
        return String.format("%.08f s", TimeUtilities.nsToS(ns));
    }
}
