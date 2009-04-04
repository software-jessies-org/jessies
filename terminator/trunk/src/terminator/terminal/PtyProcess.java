package terminator.terminal;

import e.util.*;
import java.awt.Dimension;
import java.io.*;
import java.util.concurrent.*;
import java.util.regex.*;

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
    
    private int fd = -1;
    private long pid;
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
        
        final int signal = exitValue;
        String signalDescription = "signal " + signal;
        String signalMap = System.getProperty("org.jessies.terminator.signalMap");
        Matcher matcher = Pattern.compile("\\b" + signal + ":(.+?)\\b").matcher(signalMap);
        if (matcher.find()) {
            String signalName = "SIG" + matcher.group(1);
            signalDescription += " (" + signalName + ")";
        }
        
        if (didDumpCore) {
            signalDescription += " --- core dumped";
        }
        return signalDescription;
    }
    
    public PtyProcess(String executable, String[] argv, String workingDirectory) throws Exception {
        ensureLibraryLoaded();
        startProcess(executable, argv, workingDirectory);
        if (pid == -1) {
            throw new IOException("Could not start process \"" + executable + "\".");
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
    
    public long getPid() {
        return pid;
    }
    
    private void startProcess(final String executable, final String[] argv, final String workingDirectory) throws Exception {
        invoke(new Callable<Exception>() {
            public Exception call() {
                try {
                    nativeStartProcess(executable, argv, workingDirectory);
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
        executorService.shutdownNow();
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
    
    private native void nativeStartProcess(String executable, String[] argv, String workingDirectory) throws IOException;
    private native void nativeWaitFor() throws IOException;
    public native void destroy() throws IOException;
    
    private native int nativeRead(byte[] destination, int arrayOffset, int desiredLength) throws IOException;
    private native void nativeWrite(byte[] bytes, int arrayOffset, int byteCount) throws IOException;
    
    public native void sendResizeNotification(Dimension sizeInChars, Dimension sizeInPixels) throws IOException;
    
    private native String nativeListProcessesUsingTty() throws IOException;
}
