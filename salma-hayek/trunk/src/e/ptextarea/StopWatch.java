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
        long timeTaken = System.currentTimeMillis() - lastTime;
        System.err.println(prefix + reason + " in " + timeTaken + "ms");
        updateLastTime();
    }
}
