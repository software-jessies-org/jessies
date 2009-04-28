package e.util;

import java.io.*;
import java.util.*;
import org.jessies.os.*;

/**
 * Finds files in a directory tree.
 * 
 * FIXME: parallelize internally.
 * FIXME: allow for parallelism in the caller. return a blocking iterator? accept a Processor functor?
 */
public class FileFinder {
    private boolean includeDirectories = false;
    
    /**
     * Used to filter results.
     * We could use java.io.FileFilter, but having our own interface lets us pass in the Stat.
     * It's tempting to just have one method, but having two makes it clear that the caller needs to think about directories.
     * We could also add explicit interface for asking about symbolic links (instead of calling acceptFile).
     */
    public interface Filter {
        /**
         * Return true if 'file' should be included in the resulting list of files, false to ignore the file.
         * This method will be called for all non-directory names in the file system (not just regular files).
         */
        public boolean acceptFile(File file, Stat stat);
        
        /** Return true if 'directory' should be recursed into, false otherwise. */
        public boolean enterDirectory(File directory, Stat stat);
    }
    
    private static class DefaultFilter implements Filter {
        public boolean acceptFile(File file, Stat stat) {
            return true;
        }
        
        public boolean enterDirectory(File directory, Stat stat) {
            return true;
        }
    }
    
    public FileFinder() {
    }
    
    /**
     * Whether or not the result of filesUnder should include directories.
     * Defaults to false.
     * Directories rejected by any Filter's enterDirectory are never included.
     * Directories are never passed to acceptFile.
     */
    public FileFinder includeDirectories(boolean includeDirectories) {
        this.includeDirectories = includeDirectories;
        return this;
    }
    
    /**
     * Returns all files under 'root', entering all directories and accepting all files.
     */
    public List<File> filesUnder(File root) {
        return filesUnder(root, null);
    }
    
    /**
     * Returns files under 'root', using 'filter' to decide which directories to enter and which files to accept.
     */
    public List<File> filesUnder(File root, Filter filter) {
        if (filter == null) {
            filter = new DefaultFilter();
        }
        final ArrayList<File> files = new ArrayList<File>();
        findFilesInDirectory(files, root, filter);
        return files;
    }
    
    private void findFilesInDirectory(List<File> result, File directory, Filter filter) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            final Stat stat = new Stat();
            if (Posix.lstat(file.toString(), stat) != 0) {
                // Ignore files that disappear while we're traversing the directory structure.
                continue;
            }
            if (stat.isDirectory()) {
                if (filter.enterDirectory(file, stat)) {
                    if (includeDirectories) {
                        result.add(file);
                    }
                    findFilesInDirectory(result, file, filter);
                }
            } else {
                if (filter.acceptFile(file, stat)) {
                    result.add(file);
                }
            }
        }
    }
}
