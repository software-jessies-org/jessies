package org.jessies.os;

/**
 * Process information returned by Posix.waitpid.
 * http://www.opengroup.org/onlinepubs/000095399/functions/waitpid.html
 */
public class WaitStatus {
    private int status;
    
    /** Returns the exit status. */
    public int WEXITSTATUS() {
        return PosixJNI.WExitStatus(status);
    }
    
    /** Returns true if the child has been continued. */
    public boolean WIFCONTINUED() {
        return PosixJNI.WIfContinued(status);
    }
    
    /** Returns true if the child exited normally. */
    public boolean WIFEXITED() {
        return PosixJNI.WIfExited(status);
    }
    
    /** Returns true if the child exited due to uncaught signal. */
    public boolean WIFSIGNALED() {
        return PosixJNI.WIfSignaled(status);
    }
    
    /** Returns true if child is currently stopped. */
    public boolean WIFSTOPPED() {
        return PosixJNI.WIfStopped(status);
    }
    
    /** Returns the signal number that caused process to stop. */
    public int WSTOPSIG() {
        return PosixJNI.WStopSig(status);
    }
    
    /** Returns the signal number that caused process to terminate. */
    public int WTERMSIG() {
        return PosixJNI.WTermSig(status);
    }
    
    /**
     * Returns true if the process dumped core.
     * This is non-POSIX. Returns false if information not available.
     */
    public boolean WCOREDUMP() {
        return PosixJNI.WCoreDump(status);
    }
}
