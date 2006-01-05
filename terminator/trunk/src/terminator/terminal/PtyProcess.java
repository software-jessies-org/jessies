package terminator.terminal;

import e.gui.*;
import e.util.*;
import java.awt.Dimension;
import java.io.*;
import java.util.concurrent.*;

public class PtyProcess {
    // We're compelled to do something distasteful by the inability to return an integer from JavaHpp's JNI.
    private static class PtyReadResult {
        public int bytesRead;
    }
    
    private class PtyInputStream extends InputStream {
        // InputStream compels us to implement the single byte version.
        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            return read(b, 0, 1);
        }
        /**
         * If we don't implement this variant, the default implementation
         * won't return to TerminalControl until INPUT_BUFFER_SIZE bytes
         * have been read.  We need to return as soon as a single read(2)
         * returns.
         */
        @Override
        public int read(byte[] destination, int arrayOffset, int desiredLength) throws IOException {
            PtyReadResult ptyReadResult = new PtyReadResult();
            nativeRead(destination, arrayOffset, desiredLength, ptyReadResult);
            return ptyReadResult.bytesRead;
        }
    }
    
    private class PtyOutputStream extends OutputStream {
        @Override
        public void write(int source) throws IOException {
            nativeWrite(source);
        }
    }
    
    private int fd;
    private int processId;
    
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
    
    public String getTerminatingSignal() {
        if (wasSignaled() == false) {
            throw new IllegalStateException("Process was not signaled.");
        }
        return signalDescription(exitValue);
    }
    
    private static String signalDescription(int signal) {
        String signalDescription = "signal " + signal;
        String signalName = System.getProperty("org.jessies.terminator.signal." + signal);
        if (signalName != null) {
            signalDescription += " (" + signalName + ")";
        }
        return signalDescription;
    }
    
    public PtyProcess(String[] command) throws Exception {
        ensureLibraryLoaded();
        FileDescriptor descriptor = new FileDescriptor();
        startProcess(command, descriptor);
        if (processId == -1) {
            throw new IOException("Could not start process \"" + command + "\".");
        }
        if (descriptor.valid ()) {
            inStream = new FileInputStream(descriptor);
            outStream = new FileOutputStream(descriptor);
        } else {
            inStream = new PtyInputStream();
            outStream = new PtyOutputStream();
        }
    }
    
    public InputStream getInputStream() {
        return inStream;
    }
    
    public OutputStream getOutputStream() {
        return outStream;
    }
    
    private void startProcess(final String[] command, final FileDescriptor descriptor) throws Exception {
        invoke(new Callable<Exception>() {
            public Exception call() {
                try {
                    nativeStartProcess(command, descriptor);
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
    
    private native void nativeStartProcess(String[] command, FileDescriptor descriptor) throws IOException;
    
    public native void nativeRead(byte[] destination, int arrayOffset, int desiredLength, PtyReadResult ptyReadResult) throws IOException;
    public native void nativeWrite(int source) throws IOException;
    public native void sendResizeNotification(Dimension sizeInChars, Dimension sizeInPixels) throws IOException;
    
    public native void destroy() throws IOException;
    
    private native void nativeWaitFor() throws IOException;
}
