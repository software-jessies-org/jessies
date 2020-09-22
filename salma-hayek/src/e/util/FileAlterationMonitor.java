package e.util;

import java.io.*;
import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.util.*;

/**
 * A simple cross-platform file alteration monitor.
 * Each monitor has its own thread, and uses a WatchService to keep track of file changes.
 * Only entirely directories may be watched, not individual files.
 */
public class FileAlterationMonitor {
    private String purpose;
    private WatchService watcher;
    private ArrayList<Listener> listeners = new ArrayList<>();
    
    // Map from registered path to the real (canonical) path.
    private HashMap<Path, Path> realPaths = new HashMap<>();
    
    // Map from real (canonical) path to registered watch key.
    private HashMap<Path, WatchKey> watchKeys = new HashMap<>();
    
    // Map from watch keys to the parent directories registered.
    private HashMap<WatchKey, ArrayList<Path>> keyPaths = new HashMap<>();
    
    /**
     * Constructs a new file alteration monitor.
     * The string 'purpose' is used in the thread's name to distinguish the various file alteration monitors that may be running.
     * Monitoring begins immediately.
     */
    public FileAlterationMonitor(String purpose) {
        this.purpose = purpose;
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException ex) {
            Log.warn("Failed to start file watcher for " + purpose, ex);
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        WatchKey key = watcher.take();
                        ArrayList<Path> parents = keyPaths.get(key);
                        HashSet<Path> done = new HashSet<>();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            // Ugly, but safely casting in Java seems not to be possible (or at least, I've run out
                            // of patience trying to figure out the relevant magic). This is always going to be a Path.
                            @SuppressWarnings("unchecked")
                            WatchEvent<Path> pEvent = (WatchEvent<Path>)event;
                            Path path = pEvent.context();
                            for (Path parent : parents) {
                                Path fullPath = Paths.get(parent.toString(), path.toString());
                                if (done.contains(fullPath)) {
                                    continue;
                                }
                                done.add(fullPath);
                                fireFileTouched(fullPath);
                            }
                        }
                        key.reset();
                    } catch (ClosedWatchServiceException ex) {
                        // This only happens when the watcher is shut down.
                        return;
                    } catch (InterruptedException ex) {
                        Log.warn("Interrupted file monitor " + purpose, ex);
                    }
                }
            }
        }, "FileAlterationMonitor for " + purpose).start();
    }
    
    /** Adds a directory to watch for changes (copes with "friendly" names like ~/bin). */
    public synchronized void addPathname(String pathname) {
        addPath(FileUtilities.pathFrom(pathname));
    }
    
    /** Adds a directory to watch for changes (you can safely add the same path several times). */
    public synchronized void addPath(Path path) {
        try {
            if (realPaths.containsKey(path)) {
                return;  // Already registered.
            }
            Path realPath = path.toRealPath();
            realPaths.put(path, realPath);
            WatchKey key = watchKeys.get(realPath);
            if (key != null) {
                keyPaths.get(key).add(path);
                return;
            }
            // We've never seen this real path before, so register.
            key = path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            watchKeys.put(realPath, key);
            ArrayList<Path> onePath = new ArrayList<>();
            onePath.add(path);
            keyPaths.put(key, onePath);
        } catch (Exception ex) {
            Log.warn("Failed to watch (" + purpose + ") " + path.toString(), ex);
        }
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
        try {
            watcher.close();
        } catch (IOException ex) {
            Log.warn("FileWatcher " + purpose + " failed to close", ex);
        }
        watcher = null;
        listeners = new ArrayList<Listener>();
    }
    
    private synchronized void fireFileTouched(Path path) {
        for (Listener listener : listeners) {
            listener.fileTouched(path.toString());
        }
    }
}
