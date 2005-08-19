package e.util;

import java.io.*;
import java.security.*;
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
                File otherHome = fileFromParentAndString(home.getParent(), user);
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
     * Checks solely whether anything with the given filename exists.
     * This method is equivalent to fileFromParentAndString(parent, filename).exists().
     */
    public static boolean exists(String parent, String filename) {
        return fileFromParentAndString(parent, filename).exists();
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
    
    /**
     * Closes the given Closeable, if it's non-null.
     */
    public static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Tests whether filename is a symbolic link. That is, test whether
     * the leaf is a symbolic link, not whether we need to traverse a
     * symbolic link to get to the leaf.
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
     *        => true
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
     *        => false
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
    
    private static String getUninterestingDirectoryPattern() {
        String defaultPattern = "\\.deps|\\.svn|BitKeeper|CVS|SCCS";
        String customPattern = Parameters.getParameter("directories.uninterestingNames", "");
        if (customPattern.length() > 0) {
            return customPattern + "|" + defaultPattern;
        } else {
            return defaultPattern;
        }
    }
    
    public static boolean isIgnoredDirectory(File directory) {
        if (uninterestingDirectoryNames == null) {
            uninterestingDirectoryNames = Pattern.compile(getUninterestingDirectoryPattern());
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
        for (String extension : extensions) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean nameStartsWithOneOf(String name, String[] prefixes) {
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Tests whether the given file contains ASCII text. This is done by
     * reading the first 512 bytes and ensuring that they are all
     * ASCII characters of the kind you'd expect to find in source files.
     * 
     * Really, the best way to make this kind of test is to see how many
     * bad runes we get if we interpret it as UTF-8. But that's harder,
     * and I don't actually Edit any non-ASCII files with Edit, so it
     * can wait.
     */
    public static boolean isTextFile(File file) {
        boolean isTextFile = false;
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bytes = new byte[512];
            int byteCount = fileInputStream.read(bytes);
            fileInputStream.close();
            
            int zeroByteCount = 0;
            for (int i = 0; i < byteCount; ++i) {
                if (bytes[i] == 0) {
                    ++zeroByteCount;
                }
            }
            isTextFile = (byteCount == -1 || zeroByteCount == 0);
        } catch (Exception ex) {
            ex = ex;
        }
        return isTextFile;
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
    
    /**
     * Returns the md5 digest of the given file, or null if there's a problem.
     * The string is in the same form as the digest produced by the md5sum(1)
     * command.
     */
    public static String md5(File file) {
        byte[] digest = null;
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bytes = new byte[fileInputStream.available()];
            int byteCount = fileInputStream.read(bytes);
            fileInputStream.close();
            digester.update(bytes);
            digest = digester.digest();
        } catch (Exception ex) {
            Log.warn("Unable to compute MD5 digest of '" + file + "'.", ex);
        }
        
        if (digest == null) {
            return null;
        }
        
        StringBuilder result = new StringBuilder();
        for (byte b : digest) {
            result.append(Integer.toHexString((b >> 4) & 0xf));
            result.append(Integer.toHexString(b & 0xf));
        }
        return result.toString();
    }
    
    /**
     * Returns a file found relative to the root of the salma-hayek
     * installation. This will break if FileUtilities moves package, if the
     * classes directory moves from the root of salma-hayek, or if the
     * salma-hayek classes for an application aren't actually in a copy of
     * the salma-hayek directory structure.
     */
    public static File getSalmaHayekFile(String pathFromSalmaHayekRoot) {
        String salmaHayekRoot = new File(FileUtilities.class.getResource("../..").getPath()).getParent();
        return fileFromParentAndString(salmaHayekRoot, pathFromSalmaHayekRoot);
    }
    
    public static File findOnPath(String executableName) {
        if (GuiUtilities.isWindows()) {
            executableName += ".exe";
        }
        String path = System.getenv("PATH");
        String[] directories = path.split(File.pathSeparator);
        for (String directory : directories) {
            File file = fileFromParentAndString(directory, executableName);
            if (file.exists()) {
                // FIXME: in Java 6, check for executable permission too.
                return file;
            }
        }
        return null;
    }
    
    /**
     * Creates a temporary file containing 'content' where the temporary file's
     * name begins with 'prefix'. Returns the name of the temporary file.
     * On error, a RuntimeException is thrown which will refer to the file
     * using 'humanReadableName'. The file will be deleted on exit. 
     */
    public static String createTemporaryFile(String prefix, String humanReadableName, String content) {
        try {
            File file = File.createTempFile(prefix, null);
            file.deleteOnExit();
            PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            out.print(content);
            out.close();
            return file.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Couldn't create " + humanReadableName + ": " + ex.getMessage());
        }
    }
    
    public static String getLastModifiedTime(File file) {
        return TimeUtilities.toIsoString(new Date(file.lastModified()));
    }
    
    public static void main(String[] arguments) {
        for (String filename : arguments) {
            System.err.println(parseUserFriendlyName(filename));
            System.err.println(md5(fileFromString(filename)) + "\t" + filename);
        }
    }
    
    private FileUtilities() { /* Not instantiable. */ }
}
