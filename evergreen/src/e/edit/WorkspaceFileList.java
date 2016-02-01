package e.edit;

import e.util.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.*;
import org.jdesktop.swingworker.SwingWorker;

public class WorkspaceFileList {
    private static int chooseThreadCount() {
        // A thread pool for updating the file lists.
        // One thread per workspace can cause a lot of load during startup, when all workspaces need updating at once.
        // Indeed, some machines are unusable with more than one thread doing this.
        // We don't know how to tell them from others that work fine.
        if (Evergreen.getInstance().getPreferences().getBoolean(EvergreenPreferences.MINIMIZE_INDEXING_IO)) {
            return 1;
        }
        return Runtime.getRuntime().availableProcessors();
    }
    
    private static final ExecutorService fileListUpdateExecutorService = ThreadUtilities.newFixedThreadPool(chooseThreadCount(), "File List Updater");
    
    private final Workspace workspace;
    private final ArrayList<Listener> listeners = new ArrayList<Listener>();
    
    private FileIgnorer fileIgnorer;
    private ArrayList<String> fileList;
    
    private FileAlterationMonitor fileAlterationMonitor;
    
    public WorkspaceFileList(Workspace workspace) {
        this.workspace = workspace;
    }
    
    public void addFileListListener(Listener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }
    
    public void removeFileListListener(Listener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }
    
    public void dispose() {
        fileAlterationMonitor.dispose();
    }
    
    /**
     * Returns the number of indexed files for this workspace, or -1 if no list is currently available.
     */
    public int getIndexedFileCount() {
        List<String> list = fileList;
        return (list != null) ? list.size() : -1;
    }
    
    public synchronized FileIgnorer getFileIgnorer() {
        if (fileIgnorer == null) {
            updateFileIgnorer();
        }
        return fileIgnorer;
    }
    
    private synchronized void updateFileIgnorer() {
        fileIgnorer = new FileIgnorer(FileUtilities.fileFromString(workspace.getRootDirectory()));
    }
    
    public void ensureInFileList(String pathWithinWorkspace) {
        List<String> list = fileList;
        if (list != null && list.contains(pathWithinWorkspace) == false) {
            updateFileList();
        }
    }
    
    public void rootDidChange() {
        initFileAlterationMonitorForRoot(workspace.getRootDirectory());
        updateFileList();
    }
    
    /**
     * Fills the file list. It can take some time to scan for files, so we do
     * the job in the background. New requests that arrive while a scan is
     * already in progress will be queued behind the in-progress scan.
     */
    public synchronized void updateFileList() {
        FileListUpdater fileListUpdater = new FileListUpdater();
        fileListUpdateExecutorService.execute(fileListUpdater);
    }
    
    /**
     * Returns a list of the files matching the given regular expression.
     */
    public List<String> getListOfFilesMatching(String regularExpression) {
        Pattern pattern = PatternUtilities.smartCaseCompile(regularExpression);
        ArrayList<String> result = new ArrayList<String>();
        List<String> allFiles = fileList;
        if (allFiles == null) {
            return result;
        }
        for (String candidate : allFiles) {
            Matcher matcher = pattern.matcher(candidate);
            if (matcher.find()) {
                result.add(candidate);
            }
        }
        return result;
    }
    
    private void initFileAlterationMonitorForRoot(String rootDirectory) {
        // Get rid of any existing file alteration monitor.
        if (fileAlterationMonitor != null) {
            fileAlterationMonitor.dispose();
            fileAlterationMonitor = null;
        }
        
        // Start a new thread to check for last-modified time changes...
        fileAlterationMonitor = new FileAlterationMonitor(rootDirectory);
        fileAlterationMonitor.addListener(new FileAlterationMonitor.Listener() {
            public void fileTouched(String pathname) {
                updateFileList();
            }
        });
        
        fileAlterationMonitor.addPathname(rootDirectory);
    }
    
    private class FileListUpdater extends SwingWorker<ArrayList<String>, Object> {
        private final File workspaceRoot;
        private final int prefixCharsToSkip;
        
        public FileListUpdater() {
            this.workspaceRoot = FileUtilities.fileFromString(workspace.getRootDirectory());
            // All children of the root will start with a '/', which we don't want to be part of the name.
            this.prefixCharsToSkip = workspaceRoot.toString().length() + 1;
            fireListeners(false);
            fileList = null;
        }
        
        @Override
        protected ArrayList<String> doInBackground() {
            // Don't hog the CPU while we're still getting started.
            Evergreen.getInstance().awaitInitialization();
            
            ArrayList<String> newFileList = scanWorkspaceForFiles();
            // Many file systems will have returned the files not in alphabetical order, so we sort them ourselves here.
            // Users of the list can then assume it's in order.
            Collections.sort(newFileList, String.CASE_INSENSITIVE_ORDER);
            fileList = newFileList;
            return fileList;
        }
        
        /**
         * Builds a list of files for Open Quickly.
         */
        private ArrayList<String> scanWorkspaceForFiles() {
            final long t0 = System.nanoTime();
            
            // We should reload the file ignorer's configuration when we rescan.
            updateFileIgnorer();
            
            List<File> files = new FileFinder().filesUnder(workspaceRoot, fileIgnorer);
            ArrayList<String> result = new ArrayList<String>(files.size());
            for (File file : files) {
                result.add(file.toString().substring(prefixCharsToSkip));
            }
            
            Evergreen.getInstance().showStatus("Scan of workspace \"" + workspace.getWorkspaceName() + "\" complete (" + result.size() + " files)");
            
            final long t1 = System.nanoTime();
            Log.warn("Scan of workspace \"" + workspace.getWorkspaceName() + "\" took " + TimeUtilities.nsToString(t1 - t0) + "; found " + result.size() + " files.");
            return result;
        }
        
        @Override
        public void done() {
            fireListeners(true);
        }
    }
    
    private void fireListeners(final boolean isNowValid) {
        synchronized (listeners) {
            for (final Listener l : listeners) {
                // Ensure we're running on the EDT.
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        l.fileListStateChanged(isNowValid);
                    }
                });
            }
        }
    }
    
    public interface Listener {
        /**
         * Invoked to notify listeners of the file list state.
         * Calls do not necessarily imply a change of state since the last notification.
         * This class ensures that you will be called back on the EDT.
         */
        public void fileListStateChanged(boolean isNowValid);
    }
}
