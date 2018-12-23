package e.edit;

import e.util.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.jessies.os.*;

public class FileIgnorer implements FileFinder.Filter {
    /** The tree we're responsible for, or null. */
    private final File rootDirectory;
    
    /** Extensions of files that shouldn't be indexed. */
    private final List<String> ignoredExtensions;
    
    /** Names of directories that shouldn't be entered when indexing. */
    private final Pattern uninterestingDirectoryNames;
    
    // Whether to include symbolic links even if the link's file name is the same as that of the target.
    private boolean includeAllSymbolicLinks = false;
    
    public FileIgnorer() {
        this(null);
    }
    
    public FileIgnorer(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.uninterestingDirectoryNames = makeUninterestingDirectoryPattern();
        this.ignoredExtensions = Collections.unmodifiableList(makeIgnoredExtensions());
    }
    
    public FileIgnorer includeAllSymbolicLinks(boolean includeAllSymbolicLinks) {
        this.includeAllSymbolicLinks = includeAllSymbolicLinks;
        return this;
    }
    
    private boolean isSymbolicLinkAcceptable(File file) {
        if (includeAllSymbolicLinks) {
            return true;
        }
        try {
            // Ignoring symbolic links with different file names misleadingly suggests that name isn't available.
            return file.getCanonicalFile().getName().equals(file.getName()) == false;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
}
    
    // FileFinder.Filter API.
    public boolean acceptFile(File file, Stat stat) {
        return !isIgnored(file, false) && (!stat.isSymbolicLink() || isSymbolicLinkAcceptable(file));
    }
    
    // FileFinder.Filter API.
    public boolean enterDirectory(File directory, Stat stat) {
        return !isIgnored(directory, true) && (!stat.isSymbolicLink() || isSymbolicLinkAcceptable(directory));
    }
    
    private boolean isIgnored(File file, boolean isDirectory) {
        String filename = file.getName();
        // FIXME: if it were cheap, we'd use File.isHidden. But it's unnecessarily expensive on Unix and not obviously useful on Windows.
        // (Subversion for Windows doesn't use the hidden bit for its .svn directories, for example.)
        if (filename.startsWith(".") || filename.endsWith("~")) {
            return true;
        }
        if (isDirectory) {
            // Making this a no-op for a large tree with no files to ignore makes no measurable improvement to the overall indexing time.
            return uninterestingDirectoryNames.matcher(filename).matches();
        }
        // People using ctags(1) don't want their tags files indexed.
        if (filename.equals("tags")) {
            return true;
        }
        return isIgnoredExtension(filename);
    }
    
    private ArrayList<String> makeIgnoredExtensions() {
        ArrayList<String> result = new ArrayList<>();
        result.addAll(Arrays.asList(Evergreen.getInstance().getPreferences().getString(EvergreenPreferences.UNINTERESTING_EXTENSIONS).split(";")));
        appendLinesFromScriptOutput(result, "echo-local-extensions-evergreen-should-not-index");
        return result;
    }
    
    public boolean isIgnoredExtension(String filename) {
        return nameEndsWithOneOf(filename, ignoredExtensions);
    }
    
    private Pattern makeUninterestingDirectoryPattern() {
        ArrayList<String> patterns = new ArrayList<>();
        
        // Start with the default ignored directory patterns.
        // autotools directories:
        patterns.add("\\.deps");
        patterns.add("autom4te\\.cache");
        // cmake's equivalent:
        patterns.add("CMakeFiles");
        // SCM directories:
        patterns.add("\\.bzr");
        patterns.add("\\.hg");
        patterns.add("\\.svn");
        patterns.add("BitKeeper"); patterns.add("PENDING"); patterns.add("RESYNC");
        patterns.add("CVS");
        patterns.add("SCCS");
        
        appendLinesFromScriptOutput(patterns, "echo-local-directories-evergreen-should-not-index");
        
        // Make a regular expression.
        return Pattern.compile(StringUtilities.join(patterns, "|"));
    }
    
    // FIXME: find a way to get rid of this without anything more than once-off inconvenience to its users.
    // FIXME: (then rename this class and move it to salma-hayek.)
    private void appendLinesFromScriptOutput(ArrayList<String> lines, String scriptName) {
        if (rootDirectory == null) {
            return;
        }
        
        String[] command = ProcessUtilities.makeShellCommandArray(scriptName);
        ArrayList<String> errors = new ArrayList<>();
        // Try to run any site-local script.
        ProcessUtilities.backQuote(rootDirectory, command, lines, errors);
    }
    
    /**
     * This looks stupid, but for Evergreen's purposes works faster than:
     * 1. extracting the extension from each filename and using a set.
     * 2. constructing a large pattern from the extensions and using Matcher.matches.
     * The second alternative comes close with a few tricks, but doesn't seem worth the complexity.
     * Most importantly, making this a no-op for a large tree with no files to ignore (which is the most expensive case for this method) makes no measurable improvement.
     * 
     * So this method is not worth optimizing, tempting though it may look!
     */
    private static boolean nameEndsWithOneOf(String name, List<String> extensions) {
        for (String extension : extensions) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
