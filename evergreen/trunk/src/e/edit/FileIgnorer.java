package e.edit;

import java.io.*;
import java.util.regex.*;
import e.util.*;

public class FileIgnorer {
  /** Extensions that shouldn't be shown in directory windows. */
  private String[] ignoredExtensions;
  
  /** Names of directories that shouldn't be shown in directory windows. */
  private Pattern uninterestingDirectoryNames;
  
  public FileIgnorer(String rootDirectory) {
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
  
  private static String getUninterestingDirectoryPattern(String rootDirectory) {
    String defaultPattern = "\\.deps|\\.svn|BitKeeper|CVS|SCCS";
    String customPattern = Parameters.getParameter("directories.uninterestingNames", "");
    if (customPattern.length() > 0) {
      return customPattern + "|" + defaultPattern;
    } else {
      return defaultPattern;
    }
  }
  
  public boolean isIgnoredDirectory(File directory) {
    return uninterestingDirectoryNames.matcher(directory.getName()).matches();
  }
}
