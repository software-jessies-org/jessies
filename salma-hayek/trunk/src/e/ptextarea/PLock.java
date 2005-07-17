package e.ptextarea;

import java.util.*;
import e.util.*;

/**
 * A PLock manages the locking for a PTextArea, although it could likely be used for any
 * object which needs to follow the same locking rules.
 * 
 * The locking rules are as follows:
 * 1: Only one thread may gain the write lock at any time.
 * 2: While the write lock is held, no read locks are given except to the thread which holds the write lock.
 * 3: Many read locks can be given.
 * 4: While at least one read lock is held, no write lock may be given, except to the thread which holds the
 *     read lock when it is the only one holding a read lock.
 * 
 * Using this class correctly is very important.  Failure to do so will result in the whole text area locking up.
 * Follow this pattern:
 * 
 * public void myMethod(PTextArea area) {
 *     area.getLock().getReadLock();
 *     try {
 *         doSomething();
 *     } finally {
 *         area.getLock().relinquishReadLock();
 *     }
 * }
 *
 * Locks are reference counted, so it is safe to get and relinquish a lock in a nested fashion.
 *
 * @author Phil Norman
 */

public class PLock {
    private Map<Thread, Integer> readLocks = new HashMap<Thread, Integer>();
    private Thread writeLock = null;
    private int writeLockCount = 0;
    
    public synchronized void getReadLock() {
        Thread currentThread = Thread.currentThread();
        long start = System.currentTimeMillis();
        boolean gotStuck = false;
        while (canClaimReadLock(currentThread) == false) {
            gotStuck = true;
            try {
                wait();
            } catch (InterruptedException ex) {
                Log.warn("Interrupted while attempting to get read lock.", ex);
            }
        }
        if (gotStuck) {
            long time = System.currentTimeMillis() - start;
            Log.warn("Thread " + currentThread + " waited to get read lock for " + time + "ms.");
        }
        if (readLocks.containsKey(currentThread)) {
            readLocks.put(currentThread, 1 + readLocks.get(currentThread));
        } else {
            readLocks.put(currentThread, 1);
        }
    }
    
    private boolean canClaimReadLock(Thread currentThread) {
        return ((writeLock == null) || (writeLock == currentThread));
    }
    
    public synchronized void relinquishReadLock() {
        Thread currentThread = Thread.currentThread();
        if (readLocks.containsKey(currentThread) == false) {
            throw new RuntimeException("Cannot relinquish read lock on thread " + currentThread + " because it does not hold a lock.");
        }
        int newLockCount = readLocks.get(currentThread) - 1;
        if (newLockCount == 0) {
            readLocks.remove(currentThread);
        } else {
            readLocks.put(currentThread, newLockCount);
            notifyAll();  // IMPORTANT: allow other threads to wake up and check if they can get locks now.
        }
    }
    
    public synchronized void getWriteLock() {
        Thread currentThread = Thread.currentThread();
        long start = System.currentTimeMillis();
        boolean gotStuck = false;
        while (canClaimWriteLock(currentThread) == false) {
            gotStuck = true;
            try {
                wait();
            } catch (InterruptedException ex) {
                Log.warn("Interrupted while attempting to get write lock.", ex);
            }
        }
        if (gotStuck) {
            long time = System.currentTimeMillis() - start;
            Log.warn("Thread " + currentThread + " waited to get read lock for " + time + "ms.");
        }
        writeLock = currentThread;
        writeLockCount++;
    }
    
    private boolean canClaimWriteLock(Thread currentThread) {
        if (writeLock == currentThread) {
            return true;
        } else if (writeLock != null) {
            return false;
        } else if (readLocks.size() == 0) {
            return true;
        } else if (readLocks.size() > 1) {
            return false;
        } else {
            return readLocks.containsKey(currentThread);
        }
    }
    
    public synchronized void relinquishWriteLock() {
        Thread currentThread = Thread.currentThread();
        if (writeLock != currentThread) {
            throw new RuntimeException("Cannot relinquish write lock on thread " + currentThread + " because it does not hold the lock.");
        }
        writeLockCount--;
        if (writeLockCount == 0) {
            writeLock = null;
            notifyAll();  // IMPORTANT: allow other threads to wake up and check if they can get locks now.
        }
    }
}
