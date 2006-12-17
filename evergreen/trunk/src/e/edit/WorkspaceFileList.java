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
    
    public WorkspaceFileList(Workspace workspace) {
        this.workspace = workspace;
    }
    
    public void dispose() {
        fileAlterationMonitor.dispose();
    }
    
    /**
     * Returns the number of indexed files for this workspace.
     */
    public int getIndexedFileCount() {
        return fileList.size();
    }
    
    public void ensureInFileList(String pathWithinWorkspace) {
        if (fileList != null && fileList.indexOf(pathWithinWorkspace) == -1) {
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
    public synchronized void updateFileList(ChangeListener listener) {
        FileListUpdater fileListUpdater = new FileListUpdater(listener);
        fileListUpdateExecutorService.execute(fileListUpdater);
    }
    
    public synchronized void updateFileList() {
        updateFileList(null);
    }
    
    /**
     * Checks whether the file list is available, and if so, if it's non-empty.
     * It's usually unavailable if the workspace hasn't been scanned yet.
     * The user will be shown an appropriate warning in case of unsuitability.
     */
    public boolean isFileListUnsuitableFor(String purpose) {
        if (fileList == null) {
            Evergreen.getInstance().showAlert("Can't " + purpose, "The list of files for " + workspace.getTitle() + " is not yet available.");
            return true;
        }
        if (fileList.isEmpty()) {
            Evergreen.getInstance().showAlert("Can't " + purpose, "The list of files for " + workspace.getTitle() + " is empty.");
            return true;
        }
        return false;
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
        private ChangeListener listener;
        
        public FileListUpdater(ChangeListener listener) {
            this.listener = listener;
            fileList = null;
        }
        
        @Override
        protected ArrayList<String> doInBackground() {
            ArrayList<String> newFileList = scanWorkspaceForFiles();
            // Many file systems will have returned the files not in
            // alphabetical order, so we sort them ourselves here.
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
            long start = System.currentTimeMillis();
            
            FileIgnorer fileIgnorer = new FileIgnorer(workspace.getRootDirectory());
            ArrayList<String> result = new ArrayList<String>();
            scanDirectory(workspace.getRootDirectory(), fileIgnorer, result);
            Evergreen.getInstance().showStatus("Scan of '" + workspace.getRootDirectory() + "' complete (" + result.size() + " files)");
            
            Log.warn("Scan of " + workspace.getRootDirectory() + " took " + (System.currentTimeMillis() - start) + "ms; found " + result.size() + " files.");
            return result;
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
                String filename = file.toString();
                if (file.isDirectory()) {
                    scanDirectory(filename, fileIgnorer, result);
                } else {
                    if (FileUtilities.isSymbolicLink(file) == false) {
                        int prefixCharsToSkip = FileUtilities.parseUserFriendlyName(workspace.getRootDirectory()).length();
                        result.add(filename.substring(prefixCharsToSkip));
                    }
                }
            }
        }
        
        @Override
        public void done() {
            if (listener != null) {
                listener.stateChanged(null);
            }
        }
    }
    
    public interface Listener {
        /** Invoked when the list is about to be updated. */
        public void listWillUpdate();
        /** Invoked when the list has finished being updated. */
        public void listDidUpdate();
    }
}
