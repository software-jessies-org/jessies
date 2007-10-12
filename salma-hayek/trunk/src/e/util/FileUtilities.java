package e.util;

import java.io.*;
import java.nio.channels.*;
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
        if (filename.startsWith("~" + File.separator) || filename.equals("~")) {
            result = getUserHomeDirectory();
            if (filename.length() > 1) {
                result += File.separator + filename.substring(2);
            }
        } else if (filename.startsWith("~")) {
            // Assume that "~user/bin/vi" is equivalent to "~/../user/bin/vi".
            Pattern pattern = Pattern.compile("^~([^/]+)(.*)$");
            Matcher matcher = pattern.matcher(filename);
            if (matcher.find()) {
                String user = matcher.group(1);
                File home = fileFromString(getUserHomeDirectory());
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
        String home = getUserHomeDirectory();
        if (filename.startsWith(home)) {
            return "~" + filename.substring(home.length());
        }
        return filename;
    }
    
    public static String getUserFriendlyName(File file) {
        return getUserFriendlyName(file.getAbsolutePath());
    }
    
    /**
     * Returns the user's home directory. Assumes that on Cygwin a user
     * who's set $HOME wants it to override Windows' notion of the home
     * directory, which is what the "user.home" system property gets you.
     * Removes any trailing File.separators.
     */
    public static String getUserHomeDirectory() {
        String result = System.getenv("HOME");
        if (result == null) {
            result = System.getProperty("user.home");
        }
        if (result != null && result.endsWith(File.separator)) {
            result = result.replaceAll(File.separator + "+$", "");
        }
        return result;
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
            return "Directory \"" + name + "\" does not exist.";
        } else if (proposedDirectory.isDirectory() == false) {
            return "The path \"" + name + "\" exists but does not refer to a directory.";
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
     *   upFollowDown := "/u/u58/martind/kipper/include/PtrTraits.h"
     *   follow       := "/u/u58/martind/kipper/libs/RCPtr/PtrTraits.h"
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
     *   upFollowDown := "/u/u58/martind/kipper/include"
     *   follow       := "/u/u58/martind/kipper/include"
     *        => false
     * 
     * Finally, consider the case where the file is a symbolic link
     * within the directory.  The link in question is:
     *   "/home/martind/playpen/symlink"
     * which has as its link target:
     *   "/home/martind/playpen/target"
     * In isSymbolicLink("/home/martind/playpen/symlink"):
     *   up           := "/home/martind/playpen"
     *   upThenFollow := "/home/martind/playpen"
     *   upFollowDown := "/home/martind/playpen/symlink"
     *   follow       := "/home/martind/playpen/target"
     *        => true
     */
    public static boolean isSymbolicLink(String filename) {
        File file = FileUtilities.fileFromString(filename);
        return isSymbolicLink(file);
    }
    public static boolean isSymbolicLink(File file) {
        if (file.exists() == false) {
            // There are more causes of non-existent files than dangling
            // and cyclic symbolic links but, like Java, we're happy to treat
            // them the same.
            return true;
        }
        try {
            File up = file.getAbsoluteFile().getParentFile();
            File upThenFollow = up.getCanonicalFile();
            File upFollowDown = new File(upThenFollow, file.getName());
            File follow = file.getCanonicalFile();
            return !follow.equals(upFollowDown);
        } catch (IOException ex) {
            return false;
        }
    }
    
    /**
     * Tests whether the given file contains ASCII text. This is done by
     * reading the first 512 bytes and ensuring that they are all
     * ASCII characters of the kind you'd expect to find in source files.
     * 
     * Really, the best way to make this kind of test is to see how many
     * bad runes we get if we interpret it as UTF-8. But that's harder,
     * and I don't actually edit any non-ASCII files with Evergreen, so it
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
            Log.warn("Unable to compute MD5 digest of \"" + file + "\".", ex);
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
    
    public static File findOnPath(String executableName) {
        File result = findOnPath0(executableName);
        if (result == null && GuiUtilities.isWindows()) {
            result = findOnPath0(executableName + ".exe");
        }
        return result;
    }
    
    private static File findOnPath0(String executableName) {
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
     * Finds the given script inside the given bundle.
     * 
     * On OSes other than Mac OS, simply returns the given script name.
     * The script will have to be on the user's path.
     * 
     * On Mac OS, users typically install things where they please.
     * /Applications, ~/Applications, and ~/Desktop are popular, but we could be installed anywhere.
     * We can also be moved after installation.
     * So: we call an external utility to find out where the OS last saw our .app bundle.
     */
    public static String findScriptFromBundle(String scriptName, String bundleId) {
        // On non-Mac OS systems, we have to hope the script is on the user's path.
        // (Developers on Mac OS are likely to want a script on their path to override any installed bundle too.)
        if (FileUtilities.findOnPath(scriptName) != null) {
            return scriptName;
        }
        if (GuiUtilities.isMacOs()) {
            ArrayList<String> bundleLocations = new ArrayList<String>();
            String[] command = ProcessUtilities.makeShellCommandArray(FileUtilities.findOnPath("LSFindApplicationForInfo") + " " + bundleId);
            int status = ProcessUtilities.backQuote(null, command, bundleLocations, new ArrayList<String>());
            if (status == 0 && bundleLocations.size() == 1) {
                // The app name isn't necessarily the lower-cased last element of the bundle id, but it happens to be at the moment.
                // FIXME: an alternative here would be to try all globs of Resources/*/bin/.
                String[] bundleIdComponents = bundleId.split("\\.");
                String appName = bundleIdComponents[bundleIdComponents.length - 1].toLowerCase();
                // Poke inside the bundle and return the full path to the script, if we find it.
                String scriptLocation = bundleLocations.get(0) + "/Contents/Resources/" + appName + "/bin/" + scriptName;
                if (FileUtilities.exists(scriptLocation)) {
                    return scriptLocation;
                }
            }
        }
        // If we get this far, we've failed.
        // Returning scriptName improves the chances that we'll provide a sensible error message.
        // Alternatively, we could change all callers to check for null, and return null here.
        return scriptName;
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
    
    public static void copyFile(File source, File destination) {
        try {
            // From http://java.sun.com/developer/JDCTechTips/2002/tt0507.html.
            FileInputStream fileInputStream = new FileInputStream(source);
            FileOutputStream fileOutputStream = new FileOutputStream(destination);
            FileChannel fileInputChannel = fileInputStream.getChannel();
            FileChannel fileOutputChannel = fileOutputStream.getChannel();
            
            fileInputChannel.transferTo(0, fileInputChannel.size(), fileOutputChannel);
            
            fileInputChannel.close();
            fileOutputChannel.close();
            fileInputStream.close();
            fileOutputStream.close();
        } catch (IOException ex) {
            throw new RuntimeException("Couldn't copy " + source + " to " + destination + ": " + ex.getMessage());
        }
    }

    public static void loadNativeLibrary(String libraryName) {
        String fileName = System.mapLibraryName(libraryName);
        String directories = System.getProperty("org.jessies.libraryDirectories");
        for (String directory : directories.split(File.pathSeparator)) {
            File candidatePath = new File(directory, fileName);
            try {
                System.load(candidatePath.getAbsolutePath());
                return;
            } catch (Throwable ex) {
                // Perhaps we should collect ex.getMessage() for re-throwing later
                // if we fail to load from all directories.
                ex = ex;
            }
        }
        throw new UnsatisfiedLinkError("Failed to load " + fileName + " from " + directories);
    }
    
    public static void main(String[] arguments) {
        for (String filename : arguments) {
            System.err.println(parseUserFriendlyName(filename));
            System.err.println(md5(fileFromString(filename)) + "\t" + filename);
        }
    }
    
    private FileUtilities() { /* Not instantiable. */ }
}
