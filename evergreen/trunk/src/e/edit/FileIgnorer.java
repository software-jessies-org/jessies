package e.edit;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import e.util.*;

public class FileIgnorer {
  /** Extensions of files that shouldn't be indexed. */
  private String[] ignoredExtensions;
  
  /** Names of directories that shouldn't be entered when indexing. */
  private Pattern uninterestingDirectoryNames;
  
  public FileIgnorer(String rootDirectoryPath) {
    File rootDirectory = FileUtilities.fileFromString(rootDirectoryPath);
    ignoredExtensions = FileUtilities.getArrayOfPathElements(Parameters.getParameter("files.uninterestingExtensions", ""));
    uninterestingDirectoryNames = Pattern.compile(getUninterestingDirectoryPattern(rootDirectory));
  }
  
  public boolean isIgnored(File file) {
    if (file.isHidden() || file.getName().startsWith(".") || file.getName().endsWith("~")) {
      return true;
    }
    if (file.isDirectory()) {
      return isIgnoredDirectory(file);
    }
    return FileUtilities.nameEndsWithOneOf(file, ignoredExtensions);
  }
  
  private static String getUninterestingDirectoryPattern(File rootDirectory) {
    // Add the default ignored directory patterns.
    ArrayList<String> patterns = new ArrayList<String>();
    patterns.add("\\.deps");
    patterns.add("\\.svn");
    patterns.add("BitKeeper");
    patterns.add("CVS");
    patterns.add("SCCS");
    
    // This is not a per-workspace script, it's a site-local script.
    final String scriptName = "echo-local-non-source-directory-pattern";
    String[] command = new String[] { scriptName };
    ArrayList<String> errors = new ArrayList<String>();
    ProcessUtilities.backQuote(rootDirectory, command, patterns, errors);
    
    // Make a regular expression.
    return StringUtilities.join(patterns, "|");
  }
  
  public boolean isIgnoredDirectory(File directory) {
    return uninterestingDirectoryNames.matcher(directory.getName()).matches();
  }
}
