package terminator.terminal;

import e.util.*;
import java.awt.*;
import java.io.*;

public class PtyProcess {
    private int fd;
    private int processId;
    
    private boolean didExitNormally = false;
    private boolean wasSignaled = false;
    private int exitValue;
    
    private FileInputStream inStream;
    private FileOutputStream outStream;
    
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
    
    public PtyProcess(String[] command) throws IOException {
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
    
    private native void startProcess(String[] command, FileDescriptor inDescriptor, FileDescriptor outDescriptor) throws IOException;
    
    public native void sendResizeNotification(Dimension sizeInChars, Dimension sizeInPixels) throws IOException;
    
    public native void destroy() throws IOException;
    
    public native void waitFor() throws IOException;
}
