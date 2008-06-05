package e.edit;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import e.util.*;

public class FileIgnorer {
    private File rootDirectory;
    
    /** Extensions of files that shouldn't be indexed. */
    private List<String> ignoredExtensions;
    
    /** Names of directories that shouldn't be entered when indexing. */
    private Pattern uninterestingDirectoryNames;
    
    public FileIgnorer(String rootDirectoryPath) {
        this.rootDirectory = FileUtilities.fileFromString(rootDirectoryPath);
        this.uninterestingDirectoryNames = getUninterestingDirectoryPattern();
        this.ignoredExtensions = Collections.unmodifiableList(getIgnoredExtensions());
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
    
    private ArrayList<String> getIgnoredExtensions() {
        ArrayList<String> ignoredExtensions = new ArrayList<String>();
        ignoredExtensions.addAll(Arrays.asList(Evergreen.getInstance().getPreferences().getString(EvergreenPreferences.UNINTERESTING_EXTENSIONS).split(";")));
        appendLinesFromScriptOutput(ignoredExtensions, "echo-local-extensions-evergreen-should-not-index");
        return ignoredExtensions;
    }
    
    public boolean isIgnoredExtension(String filename) {
        return FileIgnorer.nameEndsWithOneOf(filename, ignoredExtensions);
    }
    
    private Pattern getUninterestingDirectoryPattern() {
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
        
        appendLinesFromScriptOutput(patterns, "echo-local-non-source-directory-pattern");
        
        // Make a regular expression.
        return Pattern.compile(StringUtilities.join(patterns, "|"));
    }
    
    private void appendLinesFromScriptOutput(ArrayList<String> lines, String scriptName) {
        String[] command = ProcessUtilities.makeShellCommandArray(scriptName);
        ArrayList<String> errors = new ArrayList<String>();
        // Try to run any site-local script.
        ProcessUtilities.backQuote(rootDirectory, command, lines, errors);
    }
    
    public boolean isIgnoredDirectory(File directory) {
        return uninterestingDirectoryNames.matcher(directory.getName()).matches();
    }
    
    /**
     * This looks stupid, but for Evergreen's purposes works faster than:
     * 1. extracting the extension from each filename and using a set.
     * 2. constructing a large pattern from the extensions and using Matcher.matches.
     * The second alternative comes close with a few tricks, but doesn't seem worth the complexity.
     */
    public static boolean nameEndsWithOneOf(String name, List<String> extensions) {
        for (String extension : extensions) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
