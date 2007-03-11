package terminator.terminal;

import e.gui.*;
import e.util.*;
import java.awt.Dimension;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

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
        public int read(byte[] destination, int arrayOffset, int desiredLength) throws IOException {
            return nativeRead(destination, arrayOffset, desiredLength);
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
            nativeWrite(bytes, arrayOffset, byteCount);
        }
    }
    
    private int fd;
    private int processId;
    private String slavePtyName;
    
    private boolean didDumpCore = false;
    private boolean didExitNormally = false;
    private boolean wasSignaled = false;
    private int exitValue;
    
    private InputStream inStream;
    private OutputStream outStream;
    
    private static final ExecutorService executorService = ThreadUtilities.newSingleThreadExecutor("Child Forker/Reaper");
    
    private static boolean libraryLoaded = false;
    
    private static synchronized void ensureLibraryLoaded() throws UnsatisfiedLinkError {
        if (libraryLoaded == false) {
            System.loadLibrary("pty");
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
        
        final int signal = exitValue;
        String signalDescription = "signal " + signal;
        String signalName = System.getProperty("org.jessies.terminator.signal." + signal);
        if (signalName != null) {
            signalDescription += " (" + signalName + ")";
        }
        
        if (didDumpCore) {
            signalDescription += " --- core dumped";
        }
        return signalDescription;
    }
    
    public PtyProcess(String[] command, String workingDirectory) throws Exception {
        ensureLibraryLoaded();
        startProcess(command, workingDirectory);
        if (processId == -1) {
            throw new IOException("Could not start process \"" + command + "\".");
        }
        inStream = new PtyInputStream();
        outStream = new PtyOutputStream();
    }
    
    public InputStream getInputStream() {
        return inStream;
    }
    
    public OutputStream getOutputStream() {
        return outStream;
    }
    
    public int getProcessId() {
        return processId;
    }
    
    private void startProcess(final String[] command, final String workingDirectory) throws Exception {
        invoke(new Callable<Exception>() {
            public Exception call() {
                try {
                    nativeStartProcess(command, workingDirectory);
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
    
    public String listProcessesUsingTty() {
        try {
            return nativeListProcessesUsingTty();
        } catch (IOException ex) {
            Log.warn("listProcessesUsingTty failed on " + toString() + ".", ex);
            return "";
        }
    }
    
    @Override
    public String toString() {
        String result = "PtyProcess[processId=" + processId + ",fd=" + fd + ",pty=\"" + slavePtyName + "\"";
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
    
    private native void nativeStartProcess(String[] command, String workingDirectory) throws IOException;
    private native void nativeWaitFor() throws IOException;
    public native void destroy() throws IOException;
    
    private native int nativeRead(byte[] destination, int arrayOffset, int desiredLength) throws IOException;
    private native void nativeWrite(byte[] bytes, int arrayOffset, int byteCount) throws IOException;
    
    public native void sendResizeNotification(Dimension sizeInChars, Dimension sizeInPixels) throws IOException;
    
    private native String nativeListProcessesUsingTty() throws IOException;
}
