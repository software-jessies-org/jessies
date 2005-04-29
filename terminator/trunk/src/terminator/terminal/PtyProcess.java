package terminator.terminal;

import java.awt.*;
import java.io.*;

public class PtyProcess {
    private int fd;
    private int processId;
    private boolean hasTerminated;
    private int exitValue;
    private FileInputStream inStream;
    private FileOutputStream outStream;
    
    private static boolean libraryLoaded = false;
    
    private static synchronized void ensureLibrary() throws IOException {
        if (libraryLoaded == false) {
            try {
                System.loadLibrary("pty");
                libraryLoaded = true;
            } catch (UnsatisfiedLinkError error) {
                System.err.println("Serious error: cannot load libpty.");
                error.printStackTrace();
                throw new IOException("Unable to launch pty processes");
            }
        }
    }
    
    public int getExitStatus() {
        return exitValue;
    }
    
    public PtyProcess(String[] command) throws IOException {
        ensureLibrary();
        FileDescriptor inDescriptor = new FileDescriptor();
        FileDescriptor outDescriptor = new FileDescriptor();
        processId = startProcess(command, inDescriptor, outDescriptor);
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
    
    private native int startProcess(String[] command, FileDescriptor inDescriptor, FileDescriptor outDescriptor);
    
    public native void sendResizeNotification(Dimension sizeInChars, Dimension sizeInPixels);
    
    public native void destroy();
    
    public native void waitFor();
}
