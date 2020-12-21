package e.edit;

import e.util.*;
import java.awt.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.*;
import java.util.stream.*;
import javax.swing.*;

public class WorkspaceFileList {
    private final Workspace workspace;
    private final ArrayList<Listener> listeners = new ArrayList<>();
    
    private FileIgnorer fileIgnorer;
    private ArrayList<String> fileList;
     
    // How deep to allow us to go into the filesystem tree when scanning for files.
    private static final int MAX_DIR_DEPTH = 40;
    
    private FileAlterationMonitor fileAlterationMonitor;
    
    public WorkspaceFileList(Workspace workspace) {
        this.workspace = workspace;
        try (Stream<String> stream = Files.lines(workspace.getFileListCachePath())) {
            ArrayList<String> result = new ArrayList<>();
            stream.forEach(v -> result.add(v));
            fileList = result;
        } catch (Exception ex) {
            // Nothing we can do. Probably just didn't exist.
        }
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
        fileIgnorer = new FileIgnorer(workspace.getRootPath());
    }
    
    public void ensureInFileList(String pathWithinWorkspace) {
        List<String> list = fileList;
        if (list != null && list.contains(pathWithinWorkspace) == false) {
            updateFileList();
        }
    }
    
    public void rootDidChange() {
        initFileAlterationMonitorForRoot(workspace.getRootDirectory());
        if (Evergreen.getInstance().isInitialized()) updateFileList();
    }
    
    /** Checks the status of changedPath, and updates our file list in place according to its state. */
    private synchronized void partialUpdate(String changedPath) {
        if (fileList == null) {
            // File updates while we're rebuilding our list of files should be ignored.
            return;
        }
        Path path = Paths.get(changedPath);
        Path rootPath = FileUtilities.pathFrom(workspace.getRootDirectory());
        String relativePath = rootPath.relativize(path).toString();
        // NOTE: insertPoint is always negative here, because Collections.binarySearch only returns the
        // index when the matching item is equivalent, rather than merely equal.
        int insertPoint = Collections.binarySearch(fileList, relativePath, String.CASE_INSENSITIVE_ORDER);
        insertPoint = (insertPoint < 0) ? (-1 - insertPoint) : insertPoint;
        if (!Files.exists(path)) {
            // Path has disappeared, so eliminate the given file, or if this was a directory, all
            // the files underneath it.
            // Note: while it'd be far more efficient to use 'removeRange', for some reason ArrayList
            // makes that protected, so we have to remove them one by one.
            while (insertPoint < fileList.size()) {
                String entry = fileList.get(insertPoint);
                if (!entry.equals(relativePath) && !entry.startsWith(relativePath + File.separator)) {
                    break;
                }
                fileList.remove(insertPoint);
                notifyListeners(l -> l.fileDeleted(entry));
            }
            return;
        }
        // Whatever this is (file or dir), it exists.
        // If this is a normal, bog standard file, we just have to ensure it's in our file list.
        // Nothing more needs doing.
        if (Files.isRegularFile(path)) {
            if (insertPoint == fileList.size() || !fileList.get(insertPoint).equals(relativePath)) {
                // File isn't in our list yet.
                int sizeBefore = fileList.size();
                fileList.add(insertPoint, relativePath);
                notifyListeners(l -> l.fileCreated(relativePath));
            } else {
                notifyListeners(l -> l.fileChanged(relativePath));
            }
            // Add a listener firing thing here, for the specific file.
            return;
        }
        // This is either a new dir, or a symlink. In either case, just do a complete rescan. We could
        // be cleverer about dealing with such situations, but they're probably rare enough that it's not
        // worth writing the extra code to handle this case.
        updateFileList();
    }
    
    /**
     * Fills the file list. It can take some time to scan for files, so we do
     * the job in the background. New requests that arrive while a scan is
     * already in progress will be queued behind the in-progress scan.
     */
    public synchronized void updateFileList() {
        new FileListUpdater().execute();
    }
    
    /**
     * Returns a list of the files matching the given regular expression.
     */
    public List<String> getListOfFilesMatching(String regularExpression) {
        Pattern pattern = PatternUtilities.smartCaseCompile(regularExpression);
        ArrayList<String> result = new ArrayList<>();
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
    
    /**
     * Returns true if the given filename exists in this workspace.
     * The returned filename is expected to begin with the 'pretty' version of the workspace
     * path (eg "~/dev/jessies").
     */
    public boolean fileExists(String filename) {
        if (fileList == null) {
            return false;
        }
        filename = StringUtilities.trimPrefix(filename, workspace.getRootDirectory());
        // Collections.binarySearch returns the index of the element *if found*, or a negative number if it's
        // instead telling us where we should stick it.
        return Collections.binarySearch(fileList, filename, String.CASE_INSENSITIVE_ORDER) >= 0;
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
                // Ignore our own .bak files.
                if (pathname.endsWith(".bak")) {
                    return;
                }
                partialUpdate(pathname);
            }
        });
    }
    
    private class FileListUpdater extends SwingWorker<ArrayList<String>, Object> {
        private final Path workspaceRoot;
        private final int prefixCharsToSkip;
        
        public FileListUpdater() {
            this.workspaceRoot = FileUtilities.pathFrom(workspace.getRootDirectory());
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
            
            ArrayList<String> result = new ArrayList<>();
            try {
                Files.walkFileTree(workspaceRoot, EnumSet.of(FileVisitOption.FOLLOW_LINKS), MAX_DIR_DEPTH, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (fileIgnorer.enterDirectory(dir)) {
                            fileAlterationMonitor.addPath(dir);
                            return FileVisitResult.CONTINUE;
                        }
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (fileIgnorer.acceptFile(file)) {
                            result.add(file.toString().substring(prefixCharsToSkip));
                        }
                        return FileVisitResult.CONTINUE;
                    }
                    
                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException ex) {
                        // Stuff can be deleted under our feet.
                        // This is not an error, just something we cope with and continue.
                        return FileVisitResult.CONTINUE;
                    }
                });
                Evergreen.getInstance().showStatus("Scan of workspace \"" + workspace.getWorkspaceName() + "\" complete (" + result.size() + " files)");
                
                final long t1 = System.nanoTime();
                Files.write(workspace.getFileListCachePath(), result, StandardCharsets.UTF_8);
                final long t2 = System.nanoTime();
                
                Log.warn("Scan of workspace \"" + workspace.getWorkspaceName() + "\" took " + TimeUtilities.nsToString(t1 - t0) + " (plus " + TimeUtilities.nsToString(t2 - t1) + " to update cache); found " + result.size() + " files.");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
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
                GuiUtilities.invokeLater(() -> { l.fileListStateChanged(isNowValid); });
            }
        }
    }
    
    private void notifyListeners(Consumer<Listener> action) {
        synchronized (listeners) {
            for (final Listener l : listeners) {
                // Ensure we're running on the EDT.
                GuiUtilities.invokeLater(() -> action.accept(l));
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

        /**
         * Invoked when a file has been created in an incremental update.
         * The filename is the relative name under the workspace root.
         */
        public void fileCreated(String filename);

        /**
         * Invoked when a file has been changed in an incremental update.
         * The filename is the relative name under the workspace root.
         */
        public void fileChanged(String filename);

        /**
         * Invoked when a file has been deleted in an incremental update.
         * The filename is the relative name under the workspace root.
         */
        public void fileDeleted(String filename);
    }
}
