package e.edit;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import e.util.*;

public class FileIgnorer {
    /** The tree we're responsible for. */
    private final File rootDirectory;
    
    /** Extensions of files that shouldn't be indexed. */
    private final List<String> ignoredExtensions;
    
    /** Names of directories that shouldn't be entered when indexing. */
    private final Pattern uninterestingDirectoryNames;
    
    public FileIgnorer(String rootDirectoryPath) {
        this.rootDirectory = FileUtilities.fileFromString(rootDirectoryPath);
        this.uninterestingDirectoryNames = getUninterestingDirectoryPattern();
        this.ignoredExtensions = Collections.unmodifiableList(makeIgnoredExtensions());
    }
    
    public boolean isIgnored(File file, boolean isDirectory) {
        String filename = file.getName();
        // FIXME: if it were cheap, we'd use File.isHidden. But it's unnecessarily expensive on Unix and not obviously useful on Windows.
        // (Subversion for Windows doesn't use the hidden bit for its .svn directories, for example.)
        if (filename.startsWith(".") || filename.endsWith("~")) {
            return true;
        }
        if (isDirectory) {
            return isIgnoredDirectory(filename);
        }
        // People using ctags(1) don't want their tags files indexed.
        if (filename.equals("tags")) {
            return true;
        }
        return isIgnoredExtension(filename);
    }
    
    private ArrayList<String> makeIgnoredExtensions() {
        ArrayList<String> result = new ArrayList<String>();
        result.addAll(Arrays.asList(Evergreen.getInstance().getPreferences().getString(EvergreenPreferences.UNINTERESTING_EXTENSIONS).split(";")));
        appendLinesFromScriptOutput(result, "echo-local-extensions-evergreen-should-not-index");
        return result;
    }
    
    public boolean isIgnoredExtension(String filename) {
        return nameEndsWithOneOf(filename, ignoredExtensions);
    }
    
    private Pattern getUninterestingDirectoryPattern() {
        ArrayList<String> patterns = new ArrayList<String>();
        
        // Start with the default ignored directory patterns.
        // autotools directories:
        patterns.add("\\.deps");
        patterns.add("autom4te\\.cache");
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
    
    private void appendLinesFromScriptOutput(ArrayList<String> lines, String scriptName) {
        String[] command = ProcessUtilities.makeShellCommandArray(scriptName);
        ArrayList<String> errors = new ArrayList<String>();
        // Try to run any site-local script.
        ProcessUtilities.backQuote(rootDirectory, command, lines, errors);
    }
    
    public boolean isIgnoredDirectory(String filename) {
        // Making this a no-op for a large tree with no files to ignore makes no measurable improvement to the overall indexing time.
        return uninterestingDirectoryNames.matcher(filename).matches();
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
