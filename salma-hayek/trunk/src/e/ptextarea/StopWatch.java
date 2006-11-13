package e.ptextarea;

public class StopWatch {
    private long lastTime;
    private String prefix;
    
    public StopWatch() {
        this("");
    }
    
    public StopWatch(String prefix) {
        this.prefix = prefix;
        updateLastTime();
    }
    
    private void updateLastTime() {
        lastTime = System.nanoTime();
    }
    
    public void print(String reason) {
        long durationNanos = System.nanoTime() - lastTime;
        if (durationNanos > 10000) {
            System.err.println(prefix + reason + " took " + durationNanos + "ns");
        }
        updateLastTime();
    }
}
