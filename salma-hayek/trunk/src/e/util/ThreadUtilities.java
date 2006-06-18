package e.util;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class ThreadUtilities {
    /**
     * Prevents instantiation.
     */
    private ThreadUtilities() {
    }
    
    /**
     * Returns an Executor that uses a single worker thread, just like
     * {@link Executors#newSingleThreadExecutor}. The worker thread
     * will have the given name.
     */
    public static ExecutorService newSingleThreadExecutor(final String threadName) {
        return Executors.newSingleThreadExecutor(new DaemonThreadFactory() {
            public String newThreadName() {
                return threadName;
            }
        });
    }
    
    /**
     * Returns an Executor that uses a fixed-size pool of worker thread, just like
     * {@link Executors#newFixedThreadPool}. The worker thread's name is
     * poolName-thread-N, where N is the sequence number of the thread created
     * by this Executor's thread factory.
     */
    public static ExecutorService newFixedThreadPool(int size, String poolName) {
        return Executors.newFixedThreadPool(size, new NamedThreadFactory(poolName));
    }
    
    private static abstract class DaemonThreadFactory implements ThreadFactory {
        public abstract String newThreadName();
        
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, newThreadName());
            // I was going to make this a run-time option, but right now I
            // can't think why you'd ever want a worker thread to be anything
            // other than a daemon. I don't see why an idle worker thread
            // should be able to prevent the VM from exiting.
            thread.setDaemon(true);
            return thread;
        }
    }
    
    private static class NamedThreadFactory extends DaemonThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        
        NamedThreadFactory(String poolName) {
            this.namePrefix = poolName + "-thread-";
        }
        
        public String newThreadName() {
            return namePrefix + threadNumber.getAndIncrement();
         }
    }
}
