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
        lastTime = System.currentTimeMillis();
    }
    
    public void print(String reason) {
        long durationMillis = System.currentTimeMillis() - lastTime;
        if (durationMillis > 10) {
            System.err.println(prefix + reason + " took " + durationMillis + "ms");
        }
        updateLastTime();
    }
}
