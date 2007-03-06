package e.util;

import static e.util.TimeUtilities.nsToString;
import java.util.*;

/**
 * Collects timing data.
 * 
 * Use it like this:
 * 
 *   private static final Stopwatch operationStopwatch = Stopwatch.get("operation");
 * 
 *     Stopwatch.Timer timer = operationStopwatch.start();
 *     try {
 *         // Perform operation...
 *     } finally {
 *         timer.stop();
 *     }
 */
public class Stopwatch {
    private static final Map<String, Stopwatch> stopwatches = new HashMap<String, Stopwatch>();
    
    private String name;
    private int sampleCount = 0;
    private long minDuration_ns = Long.MAX_VALUE;
    private long maxDuration_ns = 0;
    private long totalDuration_ns = 0;
    
    private Stopwatch(String name) {
        this.name = name;
    }
    
    public static Stopwatch get(String name) {
        synchronized (stopwatches) {
            Stopwatch stopwatch = stopwatches.get(name);
            if (stopwatch == null) {
                stopwatch = new Stopwatch(name);
                stopwatches.put(name, stopwatch);
            }
            return stopwatch;
        }
    }
    
    private synchronized void recordTiming(long duration_ns) {
        ++sampleCount;
        if (minDuration_ns > duration_ns) {
            minDuration_ns = duration_ns;
        }
        if (maxDuration_ns < duration_ns) {
            maxDuration_ns = duration_ns;
        }
        totalDuration_ns += duration_ns;
    }
    
    public Timer start() {
        return new Timer();
    }
    
    @Override
    public String toString() {
        String result = "\"" + name + "\": ";
        if (sampleCount == 0) {
            result += " (no samples)";
        } else {
            result += StringUtilities.pluralize(sampleCount, "sample", "samples") + ", " + nsToString(totalDuration_ns) + " total, " + nsToString(minDuration_ns) + ".." + nsToString(maxDuration_ns) + " (mean " + nsToString(totalDuration_ns/(long) sampleCount) + ")";
        }
        return result;
    }
    
    public static String toStringAll() {
        Stopwatch[] all;
        synchronized (stopwatches) {
            all = stopwatches.values().toArray(new Stopwatch[stopwatches.size()]);
        }
        
        StringBuilder result = new StringBuilder();
        for (Stopwatch stopwatch : all) {
            result.append(stopwatch.toString());
            result.append("\n");
        }
        if (all.length == 0) {
            result.append("(No stopwatches.)");
        }
        return result.toString();
    }
    
    public class Timer {
        long t0_ns = System.nanoTime();
        private Timer() {
        }
        public void stop() {
            recordTiming(System.nanoTime() - t0_ns);
        }
    }
}
