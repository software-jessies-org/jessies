package terminator.terminal;

import e.gui.*;
import e.util.*;
import java.awt.Dimension;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class PtyProcess {
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
            return nativeRead(destination, arrayOffset, desiredLength);
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
    
    public PtyProcess(String[] command) throws Exception {
        ensureLibraryLoaded();
        FileDescriptor descriptor = new FileDescriptor();
        startProcess(command, descriptor);
        if (processId == -1) {
            throw new IOException("Could not start process \"" + command + "\".");
        }
        if (descriptor.valid()) {
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
    
    public int getProcessId() {
        return processId;
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
    
    public String listProcessesUsingTty() {
        if (GuiUtilities.isMacOs()) {
            return listProcessesUsingTtyOnMacOs();
        } else if (FileUtilities.findOnPath("lsof") != null) {
            return listProcessesUsingTtyWithLsof();
        }
        return "";
    }
    
    private String listProcessesUsingTtyOnMacOs() {
        try {
            return nativeListProcessesUsingTty();
        } catch (IOException ex) {
            return ""; // FIXME: return the exception message?
        }
    }
    
    private String listProcessesUsingTtyWithLsof() {
        // Linux doesn't support the sysctl(3) parameters we use on Mac OS.
        // lsof(1) takes 350ms on my Linux box right now, which is poor but just about acceptable (and 10x faster than lsof(1) on Mac OS).
        // This Java post-processing takes effectively no time, so there isn't obviously anything we can do.
        // Something like this is 10x faster still, but won't work for setuid processes:
        // ls -l /proc/*/fd/2 | perl -ne 'm|/proc/(\d+)/fd/2 -> /dev/pts/27| && system "ps --no-heading -o ucmd $1"'
        // If we used something like that, but extracted the process name from /proc/<pid>/stat, we would work on Cygwin too.
        // There's no obvious C interface we could from our JNI.
        
        // So, for now, call lsof(1).
        String[] command = new String[] { "lsof", "-w", "-Fc", slavePtyName };
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<String> errors = new ArrayList<String>();
        int status = ProcessUtilities.backQuote(null, command, lines, errors);
        if (status != 0 || errors.isEmpty() == false || (lines.size() % 2) != 0) {
            return "";
        }
        
        // And post-process its output, which looks like this:
        // $ lsof -w -Fc /dev/pts/27
        // p11974
        // cbash
        // p11991
        // cvim
        ArrayList<String> processes = new ArrayList<String>();
        for (int i = 0; i < lines.size(); i += 2) {
            String pidLine = lines.get(i);
            String nameLine = lines.get(i + 1);
            processes.add(nameLine.substring(1) + "(" + pidLine.substring(1) + ")");
        }
        return StringUtilities.join(processes, ", ");
    }
    
    private native void nativeStartProcess(String[] command, FileDescriptor descriptor) throws IOException;
    private native void nativeWaitFor() throws IOException;
    public native void destroy() throws IOException;
    
    private native int nativeRead(byte[] destination, int arrayOffset, int desiredLength) throws IOException;
    private native void nativeWrite(int source) throws IOException;
    
    public native void sendResizeNotification(Dimension sizeInChars, Dimension sizeInPixels) throws IOException;
    
    private native String nativeListProcessesUsingTty() throws IOException;
}
