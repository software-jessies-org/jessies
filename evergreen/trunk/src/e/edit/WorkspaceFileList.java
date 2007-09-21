package e.edit;

import e.util.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import javax.swing.event.*;
import org.jdesktop.swingworker.SwingWorker;

public class WorkspaceFileList {
    private Workspace workspace;
    private ArrayList<String> fileList;
    
    private FileAlterationMonitor fileAlterationMonitor;
    private String fileAlterationMonitorRoot;
    private ExecutorService fileListUpdateExecutorService;
    
    private ArrayList<Listener> listeners = new ArrayList<Listener>();
    
    public WorkspaceFileList(Workspace workspace) {
        this.workspace = workspace;
    }
    
    public void addFileListListener(Listener l) {
        listeners.add(l);
        fireListeners(fileList != null);
    }
    
    public void removeFileListListener(Listener l) {
        listeners.remove(l);
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
            fileAlterationMonitorRoot = null;
        }
        
        // We have one thread to check for last-modified time changes...
        this.fileAlterationMonitor = new FileAlterationMonitor(rootDirectory);
        this.fileAlterationMonitorRoot = rootDirectory;
        // And another thread to update our list of files...
        this.fileListUpdateExecutorService = ThreadUtilities.newSingleThreadExecutor("File List Updater for " + rootDirectory);
        
        fileAlterationMonitor.addListener(new FileAlterationMonitor.Listener() {
            public void fileTouched(String pathname) {
                updateFileList();
            }
        });
        
        fileAlterationMonitor.addPathname(rootDirectory);
    }
    
    private class FileListUpdater extends SwingWorker<ArrayList<String>, Object> {
        public FileListUpdater() {
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
            Log.warn("Scanning " + workspace.getRootDirectory() + " for interesting files.");
            final long t0 = System.nanoTime();
            
            FileIgnorer fileIgnorer = new FileIgnorer(workspace.getRootDirectory());
            ArrayList<String> result = new ArrayList<String>();
            scanDirectory(workspace.getRootDirectory(), fileIgnorer, result);
            Evergreen.getInstance().showStatus("Scan of \"" + workspace.getRootDirectory() + "\" complete (" + result.size() + " files)");
            
            Log.warn("Scan of " + workspace.getRootDirectory() + " took " + TimeUtilities.nsToString(System.nanoTime() - t0) + "; found " + result.size() + " files.");
            return result;
        }
        
        private boolean isSymbolicLinkWithinWorkspace(File file) {
            if (FileUtilities.isSymbolicLink(file) == false) {
                return false;
            }
            try {
                String canonicalFileName = file.getCanonicalFile().toString();
                String canonicalWorkspaceRootDirectory = workspace.getCanonicalRootDirectory();
                return canonicalFileName.startsWith(canonicalWorkspaceRootDirectory);
            } catch (IOException ex) {
                // (If we can't find the root directory of the workspace, then what are we scanning?)
                // If we can't find the target of a symbolic link, then we can't very well edit it.
                return true;
            }
        }
        
        private void scanDirectory(String directory, FileIgnorer fileIgnorer, ArrayList<String> result) {
            File dir = FileUtilities.fileFromString(directory);
            File[] files = dir.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                if (fileIgnorer.isIgnored(file)) {
                    continue;
                }
                if (isSymbolicLinkWithinWorkspace(file)) {
                    continue;
                }
                String filename = file.toString();
                if (file.isDirectory()) {
                    scanDirectory(filename, fileIgnorer, result);
                } else {
                    int prefixCharsToSkip = FileUtilities.parseUserFriendlyName(workspace.getRootDirectory()).length();
                    result.add(filename.substring(prefixCharsToSkip));
                }
            }
        }
        
        @Override
        public void done() {
            fireListeners(true);
        }
    }
    
    private void fireListeners(boolean isNowValid) {
        for (Listener l : listeners) {
            l.fileListStateChanged(isNowValid);
        }
    }
    
    public interface Listener {
        /**
         * Invoked to notify listeners of the file list state.
         * Calls do not necessarily imply a change of state since the last notification.
         */
        public void fileListStateChanged(boolean isNowValid);
    }
}
