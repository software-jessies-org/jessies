package e.util;

/**
 * Useful to avoid conditional code in places where you have 0 or 1 runnables.
 */
public final class NoOpRunnable implements Runnable {
    /**
     * Does nothing.
     */
    public void run() {
    }
}
