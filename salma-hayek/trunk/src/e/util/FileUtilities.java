package e.util;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Provides utilities for dealing with files and paths.
 */

public class FileUtilities {
    /**
     * Returns a new File for the given filename, coping with "~/".
     * Try not to ever use "new File": use this instead.
     */
    public static File fileFromString(String filename) {
        return new File(FileUtilities.parseUserFriendlyName(filename));
    }

    /**
     * Converts paths of the form ~/src to /Users/elliotth/src (or
     * whatever the user's home directory is). Also copes with the
     * special case of ~ on its own, but not with ~someone-else/tmp
     * because Java gives us no way to find another user's home
     * directory. We could, I suppose, compare user.home and user.name
     * and try to guess, but I'll wait until I really need the functionality
     * before I add the hack.
     */
    public static String parseUserFriendlyName(String filename) {
        String result = filename;
        if (filename.startsWith("~/") || filename.equals("~")) {
            result = System.getProperty("user.home");
            if (filename.length() > 1) {
                result += File.separator + filename.substring(2);
            }
        }
        return result;
    }
    
    /**
     * Strips the user's home directory from the beginning of the string
     * if it's there, replacing it with ~. It would be nice to do other users'
     * home directories too, but I can't think of a pure Java way to do
     * that.
     */
    public static String getUserFriendlyName(String filename) {
        String home = System.getProperty("user.home");
        if (filename.startsWith(home)) {
            return "~" + filename.substring(home.length());
        }
        return filename;
    }
    
    public static String getUserFriendlyName(File file) {
        return getUserFriendlyName(file.getAbsolutePath());
    }
    
    /**
     * Checks that a name exists and is a directory. Returns null if it does, an error suitable for a UI
     * if not.
     */
    public static String checkDirectoryExistence(String name) {
        File proposedDirectory = FileUtilities.fileFromString(name);
        if (proposedDirectory.exists() == false) {
            return "Directory '" + name + "' does not exist.";
        } else if (proposedDirectory.isDirectory() == false) {
            return "The path '" + name + "' exists but does not refer to a directory.";
        }
        return null;
    }
    
    public static void close(LineNumberReader in) {
        if (in == null) {
            return;
        }
        try {
            in.close();
        } catch (IOException ex) {
            // This method's purpose is to ignore this exception!
        }
    }
    
    public static void close(PrintWriter out) {
        if (out == null) {
            return;
        }
        out.close();
    }
    
    /*
     * Tests whether filename is a symbolic link.
     * If the absolute path and the canonical path are not
     * the same, this means we're looking at a symbolic
     * link.
     * FIXME: the trouble with this is that it only means there
     * was a symbolic link somewhere on the path. It doesn't
     * actually mean that the leaf is a symbolic link.
     */
    public static boolean isSymbolicLink(String filename) {
        File file = FileUtilities.fileFromString(filename);
        return isSymbolicLink(file);
    }
    public static boolean isSymbolicLink(File file) {
        try {
            String canonicalFilename = file.getCanonicalPath();
            String absoluteFilename = file.getAbsolutePath();
            return !absoluteFilename.equals(canonicalFilename);
        } catch (IOException ex) {
            return false;
        }
    }
    
    /** Extensions that shouldn't be shown in directory windows. */
    private static String[] ignoredExtensions;
    
    /** Names of directories that shouldn't be shown in directory windows. */
    private static Pattern uninterestingDirectoryNames;
    
    public static boolean isIgnored(File file) {
        if (file.isHidden() || file.getName().startsWith(".")) {
            return true;
        }
        if (file.isDirectory()) {
            return isIgnoredDirectory(file);
        }
        if (ignoredExtensions == null) {
            ignoredExtensions = FileUtilities.getArrayOfPathElements(Parameters.getParameter("files.uninterestingExtensions", ""));
        }
        return FileUtilities.nameEndsWithOneOf(file, ignoredExtensions);
    }
    
    public static boolean isIgnoredDirectory(File directory) {
        if (uninterestingDirectoryNames == null) {
            uninterestingDirectoryNames = Pattern.compile(Parameters.getParameter("directories.uninterestingNames", "(SCCS|CVS)"));
        }
        return uninterestingDirectoryNames.matcher(directory.getName()).matches();
    }
    
    /** Returns an array with an item for each semicolon-separated element of the path. */
    public static String[] getArrayOfPathElements(String path) {
        return path.split(";");
    }
    
    public static boolean nameEndsWithOneOf(File file, String[] extensions) {
        return nameEndsWithOneOf(file.toString(), extensions);
    }
    
    public static boolean nameEndsWithOneOf(String name, String[] extensions) {
        for (int i = 0; i < extensions.length; i++) {
            if (name.endsWith(extensions[i])) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean nameStartsWithOneOf(String name, String[] extensions) {
        for (int i = 0; i < extensions.length; i++) {
            if (name.startsWith(extensions[i])) {
                return true;
            }
        }
        return false;
    }
    
    /** Extensions that correspond to binary file types. */
    private static final String[] DEFAULT_BINARY_EXTENSIONS = new String[] {
        ".gif", ".gz", ".icns", ".jpg", ".mov", ".mp3", ".mpeg", ".mpg", ".pdf", ".tiff"
    };
    
    public static boolean isBinaryFile(File file) {
        return nameEndsWithOneOf(file, DEFAULT_BINARY_EXTENSIONS);
    }
    
    public static String findFileByNameSearchingUpFrom(String leafName, String startPath) {
        while (startPath.length() > 0) {
            String filename = startPath + File.separatorChar + leafName;
            File file = FileUtilities.fileFromString(filename);
            if (file.exists()) {
                return filename;
            }
            int lastSeparator = startPath.lastIndexOf(File.separatorChar);
            if (lastSeparator == -1) break;
            startPath = startPath.substring(0, lastSeparator);
        }
        return null;
    }
    
    private FileUtilities() { /* Not instantiable. */ }
}
