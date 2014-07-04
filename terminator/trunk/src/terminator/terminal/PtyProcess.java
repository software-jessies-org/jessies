package terminator.terminal;

import e.util.*;
import java.awt.Dimension;
import java.io.*;
import java.util.concurrent.*;
import org.jessies.os.*;

public class PtyProcess {
    private class PtyInputStream extends InputStream {
        /**
         * Although we don't want to invoke this inefficient method, it's abstract in InputStream, so we have to "implement" it.
         */
        @Override
        public int read() throws IOException {
            throw new UnsupportedOperationException();
        }
        
        /**
         * If we don't implement this variant, the default implementation
         * won't return to TerminalControl until INPUT_BUFFER_SIZE bytes
         * have been read.  We need to return as soon as a single read(2)
         * returns.
         */
        @Override
        public int read(byte[] bytes, int arrayOffset, int byteCount) throws IOException {
            int n = 0;
            while ((n = Posix.read(fd, bytes, arrayOffset, byteCount)) < 0) {
                if (n != -Errno.EINTR) {
                    throw new IOException("read(" + fd + ", buffer, " + arrayOffset + ", " + byteCount + ") failed: " + Errno.toString(-n));
                }
            }
            return n;
        }
    }
    
    private class PtyOutputStream extends OutputStream {
        /**
         * Although we don't want to invoke this inefficient method, it's abstract in OutputStream, so we have to "implement" it.
         */
        @Override
        public void write(int b) throws IOException {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void write(byte[] bytes, int arrayOffset, int byteCount) throws IOException {
            // POSIX (http://www.opengroup.org/onlinepubs/000095399/functions/write.html) says:
            // 1. we can be interrupted before any bytes are written (n == -1, errno == EINTR).
            // 2. we can be interrupted after some bytes are written (n < requested n).
            int offset = arrayOffset;
            int remainingByteCount = byteCount;
            int n = 0;
            while (remainingByteCount > 0) {
                n = Posix.write(fd, bytes, offset, byteCount);
                if (n < 0 && n != -Errno.EINTR) {
                    // This write failed, and not because we were interrupted before writing anything. Give up.
                    break;
                }
                if (n > 0) {
                    offset += n;
                    remainingByteCount -= n;
                }
            }
            if (remainingByteCount != 0) {
                throw new IOException("write(" + fd + ", buffer, " + arrayOffset + ", " + byteCount + ") failed: " + Errno.toString(-n));
            }
        }
    }
    
    private int fd = -1;
    private int pid;
    private String slavePtyName;
    
    private boolean didDumpCore = false;
    private boolean didExitNormally = false;
    private boolean wasSignaled = false;
    private int exitValue;
    
    private InputStream inStream;
    private OutputStream outStream;
    
    private final ExecutorService executorService = ThreadUtilities.newSingleThreadExecutor("Child Forker/Reaper");
    
    private static boolean libraryLoaded = false;
    
    private static synchronized void ensureLibraryLoaded() throws UnsatisfiedLinkError {
        if (libraryLoaded == false) {
            FileUtilities.loadNativeLibrary("pty");
            libraryLoaded = true;
        }
    }
    
    public boolean wasSignaled() {
        return wasSignaled;
    }
    
    public boolean didExitNormally() {
        return didExitNormally;
    }
    
    public int getExitStatus() {
        if (didExitNormally() == false) {
            throw new IllegalStateException("Process did not exit normally.");
        }
        return exitValue;
    }
    
    public String getPtyName() {
        return slavePtyName;
    }
    
    public String getSignalDescription() {
        if (wasSignaled() == false) {
            throw new IllegalStateException("Process was not signaled.");
        }
        
        String signalDescription = Signal.toString(exitValue);
        if (didDumpCore) {
            signalDescription += " --- core dumped";
        }
        return signalDescription;
    }
    
    public PtyProcess(String executable, String[] argv, String workingDirectory) throws Exception {
        ensureLibraryLoaded();
        startProcess(executable, argv, workingDirectory);
        inStream = new PtyInputStream();
        outStream = new PtyOutputStream();
    }
    
    public InputStream getInputStream() {
        return inStream;
    }
    
    public OutputStream getOutputStream() {
        return outStream;
    }
    
    public int getFd() {
        return fd;
    }
    
    public int getPid() {
        return pid;
    }
    
    private void startProcess(final String executable, final String[] argv, final String workingDirectory) throws Exception {
        invoke(new Callable<Exception>() {
            public Exception call() {
                try {
                    nativeStartProcess(executable, argv, workingDirectory);
                    updateLoginRecord();
                    return null;
                } catch (Exception ex) {
                    return ex;
                }
            }
        });
    }
    
    public void waitFor() throws Exception {
        invoke(new Callable<Exception>() {
            public Exception call() {
                try {
                    waitFor0();
                    return null;
                } catch (Exception ex) {
                    return ex;
                }
            }
        });
        executorService.shutdownNow();
    }
    
    private void waitFor0() throws IOException {
        // FIXME: rewrite this to be more like the JDK's Process.waitFor, both in behavior and implementation.
        
        // We now have no further use for the fd connecting us to the child, which has probably exited.
        // Even if it hasn't, we're no longer reading its output, which may cause the child to block in the kernel,
        // preventing it from terminating, even if root sends it SIGKILL.
        // If we close the pipe before waiting, then we may let it finish and collect an exit status.
        Posix.close(fd);
        fd = -1;
        
        // Loop until waitpid(2) returns a status or a real error.
        WaitStatus status = new WaitStatus();
        int result;
        while ((result = Posix.waitpid(pid, status, 0)) < 0) {
            if (result != -Errno.EINTR) {
                // Something really went wrong; give up.
                throw new IOException("waitpid(" + pid + ") failed: " + Errno.toString(-result));
            }
        }
        
        // Translate the status.
        if (status.WIFEXITED()) {
            exitValue = status.WEXITSTATUS();
            didExitNormally = true;
        }
        if (status.WIFSIGNALED()) {
            exitValue = status.WTERMSIG();
            wasSignaled = true;
            didDumpCore = status.WCOREDUMP();
        }
        
        updateLoginRecord();
    }
    
    /**
     * Java 1.5.0_03 on Linux 2.4.27 doesn't seem to use LWP threads (according
     * to ps -eLf) for Java threads. Linux 2.4 is broken such that only the
     * Java thread which forked a child can wait for it.
     */
    private void invoke(Callable<Exception> callable) throws Exception {
        Future<Exception> future = executorService.submit(callable);
        Exception exception = future.get();
        if (exception != null) {
            throw exception;
        }
    }
    
    public String listProcessesUsingTty() {
        try {
            return nativeListProcessesUsingTty();
        } catch (IOException ex) {
            Log.warn("listProcessesUsingTty failed on " + toString() + ".", ex);
            return "";
        }
    }
    
    @Override public String toString() {
        String result = "PtyProcess[pid=" + pid + ",fd=" + fd + ",pty=\"" + slavePtyName + "\"";
        if (didExitNormally) {
            result += ",didExitNormally,exitValue=" + exitValue;
        }
        if (wasSignaled) {
            result += ",wasSignaled,signal=" + exitValue;
        }
        if (didDumpCore) {
            result += ",didDumpCore";
        }
        result += "]";
        return result;
    }
    
    public void destroy() throws IOException {
        int rc = Posix.killpg(pid, Signal.SIGHUP);
        if (rc < 0) {
            throw new IOException("killpg(" + pid + ", SIGHUP) failed: " + Errno.toString(-rc));
        }
    }
    
    public void updateLoginRecord() {
        try {
            nativeUpdateLoginRecord();
        } catch (IOException ex) {
            Log.warn("nativeUpdateLoginRecord failed on " + toString() + ".", ex);
        }
    }
    
    private native void nativeStartProcess(String executable, String[] argv, String workingDirectory) throws IOException;
    
    public native void sendResizeNotification(Dimension sizeInChars, Dimension sizeInPixels) throws IOException;
    
    private native String nativeListProcessesUsingTty() throws IOException;
    
    private native void nativeUpdateLoginRecord() throws IOException;
}
