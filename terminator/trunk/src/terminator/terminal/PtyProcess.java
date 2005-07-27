package terminator.terminal;

import e.util.*;
import java.awt.Dimension;
import java.io.*;
import java.util.concurrent.*;

public class PtyProcess {
    private int fd;
    private int processId;
    
    private boolean didExitNormally = false;
    private boolean wasSignaled = false;
    private int exitValue;
    
    private FileInputStream inStream;
    private FileOutputStream outStream;
    
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    
    private static boolean libraryLoaded = false;
    
    private static synchronized void ensureLibrary() throws IOException {
        if (libraryLoaded == false) {
            try {
                System.loadLibrary("pty");
                libraryLoaded = true;
            } catch (UnsatisfiedLinkError ex) {
                Log.warn("Cannot load JNI library", ex);
                throw new IOException("Unable to launch pty processes");
            }
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
    
    public int getTerminatingSignal() {
        if (wasSignaled() == false) {
            throw new IllegalStateException("Process was not signaled.");
        }
        return exitValue;
    }
    
    public PtyProcess(String[] command) throws Exception {
        ensureLibrary();
        FileDescriptor inDescriptor = new FileDescriptor();
        FileDescriptor outDescriptor = new FileDescriptor();
        startProcess(command, inDescriptor, outDescriptor);
        if (processId == -1) {
            throw new IOException("Could not start process \"" + command + "\".");
        }
        inStream = new FileInputStream(inDescriptor);
        outStream = new FileOutputStream(outDescriptor);
    }
    
    public InputStream getInputStream() {
        return inStream;
    }
    
    public OutputStream getOutputStream() {
        return outStream;
    }
    
    private void startProcess(final String[] command, final FileDescriptor inDescriptor, final FileDescriptor outDescriptor) throws Exception {
        invoke(new Callable<Exception>() {
            public Exception call() {
                try {
                    nativeStartProcess(command, inDescriptor, outDescriptor);
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
                    nativeWaitFor();
                    return null;
                } catch (Exception ex) {
                    return ex;
                }
            }
        });
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
    
    private native void nativeStartProcess(String[] command, FileDescriptor inDescriptor, FileDescriptor outDescriptor) throws IOException;
    
    public native void sendResizeNotification(Dimension sizeInChars, Dimension sizeInPixels) throws IOException;
    
    public native void destroy() throws IOException;
    
    private native void nativeWaitFor() throws IOException;
}
