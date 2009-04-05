package e.util;

import java.io.*;
import java.util.*;

/**
 * A simple cross-platform file alteration monitor.
 * Each monitor has its own thread, but checks the times of the set of files it's given sequentially.
 * The intent is that no instance should have to deal with files on different file systems. The unresponsiveness of any one file system will not harm monitoring of any other file system, nor will it cause excessive numbers of threads to be created as the timer fires: all access to that file system will be blocked until the first hung call completes.
 */
public class FileAlterationMonitor {
    private ArrayList<FileDetails> files = new ArrayList<FileDetails>();
    private ArrayList<Listener> listeners = new ArrayList<Listener>();
    private Timer timer;
    
    /**
     * Constructs a new file alteration monitor.
     * The string 'purpose' is used in the thread's name to distinguish the various file alteration monitors that may be running.
     * Monitoring begins immediately.
     */
    public FileAlterationMonitor(String purpose) {
        this.timer = new Timer("FileAlterationMonitor for " + purpose, true);
        timer.schedule(new TimerTask() {
            public void run() {
                checkFileTimes();
            }
        }, 0, 1000);
    }
    
    public synchronized void addPathname(String pathname) {
        files.add(new FileDetails(pathname));
    }
    
    /**
     * The listener interface for receiving notifications when files are touched.
     */
    public interface Listener {
        /**
         * Invoked when a file is touched. The pathname returned is exactly as supplied to addPathname.
         */
        public void fileTouched(String pathname);
    }
    
    public synchronized void addListener(Listener l) {
        listeners.add(l);
    }
    
    public synchronized void removeListener(Listener l) {
        listeners.remove(l);
    }
    
    /**
     * Disposes of this file alteration manager such that it will no longer reference any pathnames or listeners, and the timer and its associated thread will be stopped.
     */
    public synchronized void dispose() {
        timer.cancel();
        files = null;
        listeners = null;
        timer = null;
    }
    
    private synchronized void checkFileTimes() {
        for (FileDetails fileDetails : files) {
            long newTime = fileDetails.file.lastModified();
            if (fileDetails.lastModified != newTime) {
                fileDetails.lastModified = newTime;
                fireFileTouched(fileDetails);
            }
        }
    }
    
    private synchronized void fireFileTouched(FileDetails fileDetails) {
        for (Listener l : listeners) {
            l.fileTouched(fileDetails.pathname);
        }
    }
    
    private static class FileDetails {
        String pathname;
        File file;
        long lastModified;
        
        FileDetails(String pathname) {
            this.pathname = pathname;
            this.file = FileUtilities.fileFromString(pathname);
            this.lastModified = file.lastModified();
        }
    }
}
