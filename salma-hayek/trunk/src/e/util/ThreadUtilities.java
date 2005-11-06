package e.util;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class ThreadUtilities {
    
    /**
     * Prevents instantiation.
     */
    private ThreadUtilities() { }
    
    /**
     * Returns an Executor that uses a single worker thread, just like
     * {@link Executors#newSingleThreadExecutor}. The worker thread's name is
     * poolName-thread-N, where N is the sequence number of the thread created
     * by this Executor's thread factory.
     */
    public static ExecutorService newSingleThreadExecutor(String poolName) {
        return Executors.newSingleThreadExecutor(new NamedThreadFactory(poolName));
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
    
    private static class NamedThreadFactory implements ThreadFactory {
        
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;
        
        NamedThreadFactory(String poolName) {
            namePrefix = poolName + "-thread-";
        }
        
        public Thread newThread(Runnable r) {
            return new Thread(r, namePrefix + threadNumber.getAndIncrement());
         }
    }
    
}
