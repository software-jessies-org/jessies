package e.edit;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import e.util.*;

public class FileIgnorer {
    /** Extensions of files that shouldn't be indexed. */
    private static String[] ignoredExtensions;
    
    /** Names of directories that shouldn't be entered when indexing. */
    private Pattern uninterestingDirectoryNames;
    
    public FileIgnorer(String rootDirectoryPath) {
        File rootDirectory = FileUtilities.fileFromString(rootDirectoryPath);
        uninterestingDirectoryNames = Pattern.compile(getUninterestingDirectoryPattern(rootDirectory));
    }
    
    public boolean isIgnored(File file) {
        String filename = file.getName();
        if (file.isHidden() || filename.startsWith(".") || filename.endsWith("~")) {
            return true;
        }
        if (file.isDirectory()) {
            return isIgnoredDirectory(file);
        }
        return isIgnoredExtension(filename);
    }
    
    public static boolean isIgnoredExtension(String filename) {
        if (ignoredExtensions == null) {
            ignoredExtensions = FileUtilities.getArrayOfPathElements(Parameters.getParameter("files.uninterestingExtensions", ""));
        }
        return FileUtilities.nameEndsWithOneOf(filename, ignoredExtensions);
    }
    
    private static String getUninterestingDirectoryPattern(File rootDirectory) {
        ArrayList<String> patterns = new ArrayList<String>();
        
        // Start with the default ignored directory patterns.
        // autotools directories:
        patterns.add("\\.deps");
        patterns.add("autom4te.cache");
        // SCM directories:
        patterns.add("\\.bzr");
        patterns.add("\\.hg");
        patterns.add("\\.svn");
        patterns.add("BitKeeper"); patterns.add("PENDING"); patterns.add("RESYNC");
        patterns.add("CVS");
        patterns.add("SCCS");
        
        // Try to run the site-local script.
        final String scriptName = "echo-local-non-source-directory-pattern";
        String[] command = ProcessUtilities.makeShellCommandArray(scriptName);
        ArrayList<String> errors = new ArrayList<String>();
        ProcessUtilities.backQuote(rootDirectory, command, patterns, errors);
        
        // Make a regular expression.
        return StringUtilities.join(patterns, "|");
    }
    
    public boolean isIgnoredDirectory(File directory) {
        return uninterestingDirectoryNames.matcher(directory.getName()).matches();
    }
}
