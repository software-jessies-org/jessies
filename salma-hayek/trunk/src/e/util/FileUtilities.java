package e.util;

import java.io.*;
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
    public static File fileFromParentAndString(String parent, String filename) {
        return fileFromString(parent + File.separator + filename);
    }

    /**
     * Converts paths of the form ~/src to /Users/elliotth/src (or
     * whatever the user's home directory is). Also copes with the
     * special case of ~ on its own, and with ~someone-else/tmp.
     */
    public static String parseUserFriendlyName(String filename) {
        String result = filename;
        if (filename.startsWith("~/") || filename.equals("~")) {
            result = System.getProperty("user.home");
            if (filename.length() > 1) {
                result += File.separator + filename.substring(2);
            }
        } else if (filename.startsWith("~")) {
            // Assume that "~user/bin/vi" is equivalent to "~/../user/bin/vi".
            Pattern pattern = Pattern.compile("^~([^/]+)(.*)$");
            Matcher matcher = pattern.matcher(filename);
            if (matcher.find()) {
                String user = matcher.group(1);
                File home = fileFromString(System.getProperty("user.home"));
                File otherHome = fileFromParentAndString(home.getParent().toString(), user);
                if (otherHome.exists() && otherHome.isDirectory()) {
                    result = otherHome.toString() + matcher.group(2);
                }
            }
        }
        return result;
    }
    
    /**
     * Strips the user's home directory from the beginning of the string
     * if it's there, replacing it with ~. It would be nice to do other users'
     * home directories too, but I can't think of a pure Java way to do
     * that.
     * Also adds a trailing separator to the name of a directory.
     */
    public static String getUserFriendlyName(String filename) {
        boolean isDirectory = fileFromString(filename).isDirectory();
        if (isDirectory && filename.endsWith(File.separator) == false) {
            filename += File.separatorChar;
        }
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
     * Checks solely whether anything with the given filename exists.
     * This method is equivalent to fileFromString(filename).exists().
     */
    public static boolean exists(String filename) {
        return fileFromString(filename).exists();
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
            ex = ex;
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
     *
     * First, consider the case where the file in question is a symbolic link.
     * Say that the symbolic link:
     *   "/u/u58/martind/kipper/include/PtrTraits.h"
     * has as its link target:
     *   "/u/u58/martind/kipper/libs/RCPtr/PtrTraits.h"
     * In isSymbolicLink("/u/u58/martind/kipper/include/PtrTraits.h"):
     *   up           := "/u/u58/martind/kipper/include"
     *   upThenFollow := "/u/u58/martind/kipper/include"
     *   follow       := "/u/u58/martind/kipper/libs/RCPtr/PtrTraits.h"
     *   followThenUp := "/u/u58/martind/kipper/libs/RCPtr"
     *
     * Now consider the case where the file in question isn't a symbolic link
     * but something on its path is - and let's pick the case that's most
     * likely to cause us problems - the one where our immediate parent is a
     * symbolic link.  The link in question is:
     *   "/home/martind/kipper"
     * which has as its link target:
     *   "/u/u58/martind/kipper"
     * In isSymbolicLink("/home/martind/kipper/include"):
     *   up           := "/home/martind/kipper"
     *   upThenFollow := "/u/u58/martind/kipper"
     *   follow       := "/u/u58/martind/kipper/include"
     *   followThenUp := "/u/u58/martind/kipper"
     */
    public static boolean isSymbolicLink(String filename) {
        File file = FileUtilities.fileFromString(filename);
        return isSymbolicLink(file);
    }
    public static boolean isSymbolicLink(File file) {
        try {
            File up = new File(file.getAbsolutePath());
            String upThenFollow = up.getParentFile().getCanonicalPath();
            File follow = new File(file.getCanonicalPath());
            String followThenUp = follow.getParent();
            return !upThenFollow.equals(followThenUp);
        } catch (IOException ex) {
            return false;
        }
    }
    
    /** Extensions that shouldn't be shown in directory windows. */
    private static String[] ignoredExtensions;
    
    /** Names of directories that shouldn't be shown in directory windows. */
    private static Pattern uninterestingDirectoryNames;
    
    public static boolean isIgnored(File file) {
        if (file.isHidden() || file.getName().startsWith(".") || file.getName().endsWith("~")) {
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
    
    public static void main(String[] args) {
        for (int i = 0; i < args.length; ++i) {
            System.err.println(parseUserFriendlyName(args[i]));
        }
    }
    
    private FileUtilities() { /* Not instantiable. */ }
}
