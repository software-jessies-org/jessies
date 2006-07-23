package e.edit;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import e.ptextarea.*;
import e.util.*;

/**
 * Offers man page entries corresponding to selected words.
 */
public class ManPageResearcher implements WorkspaceResearcher {
    /** Our manual is a mapping from section names to sets of page names. */
    private static Map<String, Set<String>> manual;
    
    private static TreeSet<String> uniqueWords;
    
    private static ManPageResearcher INSTANCE = new ManPageResearcher();
    
    private ManPageResearcher() {
    }
    
    public synchronized static ManPageResearcher getSharedInstance() {
        if (manual == null) {
            init();
        }
        return INSTANCE;
    }

    /**
     * Initializes the set of known man pages.
     */
    private static void init() {
        final long startTime = System.currentTimeMillis();
        
        manual = new HashMap<String, Set<String>>();
        TreeSet<String> uniqueIdentifiers = new TreeSet<String>();
        
        int pageCount = 0;
        Pattern manPagePattern = Pattern.compile("^(.*)\\.([23][A-Za-z]*)(\\.gz)?$");
        for (File manPath : findManPageDirectories()) {
            String[] manPages = manPath.list();
            if (manPages == null) manPages = new String[0];
            for (String manPage : manPages) {
                Matcher matcher = manPagePattern.matcher(manPage);
                if (matcher.matches()) {
                    String stub = matcher.group(1);
                    String sectionName = matcher.group(2);
                    if (sectionName.endsWith("pm") || sectionName.endsWith("tcl") || sectionName.endsWith("ssl")) {
                        continue;
                    }
                    Set<String> section = manual.get(sectionName);
                    if (section == null) {
                        section = new HashSet<String>();
                        manual.put(sectionName, section);
                    }
                    section.add(stub);
                    uniqueIdentifiers.add(stub);
                    ++pageCount;
                } else {
                    Log.warn("unexpected man page \"" + manPage + "\"");
                }
            }
        }
        
        // FIXME: this turns "posix_openpt" into two words, so the spelling checker will accept "openpt" alone, rather than just in the identifier "posix_openpt" as intended. Maybe we should check blessed identifiers as a whole before we try break them into words, and then supply the spelling checker with the unique identifiers we bless, rather than just the list of words? (At the same time, passing all the words works well for Java source.)
        uniqueWords = JavaResearcher.extractUniqueWords(uniqueIdentifiers.iterator());
        
        final long duration = System.currentTimeMillis() - startTime;
        Log.warn("Learned of " + pageCount + " man pages in " + duration + "ms.");
    }
    
    private static List<File> findManPageDirectories() {
        ArrayList<File> manPaths = new ArrayList<File>();
        Pattern manPathPattern = Pattern.compile("^(?:MANDATORY_)?MANPATH\\s+(.*)$");
        String[] configurationPossibilities = new String[] { "/usr/share/misc/man.conf", "/etc/manpath.config" };
        for (String configurationPossibility : configurationPossibilities) {
            if (FileUtilities.exists(configurationPossibility)) {
                for (String line : StringUtilities.readLinesFromFile(configurationPossibility)) {
                    Matcher matcher = manPathPattern.matcher(line);
                    if (matcher.matches()) {
                        manPaths.add(new File(matcher.group(1), "man2"));
                        manPaths.add(new File(matcher.group(1), "man3"));
                    }
                }
            }
        }
        return manPaths;
    }
    
    public void addManPageWordsTo(Set<String> set) {
        set.addAll(uniqueWords);
    }
    
    public String research(String string) {
        for (String sectionName : manual.keySet()) {
            Set<String> section = manual.get(sectionName);
            if (section.contains(string)) {
                return "Manual page for <a href=\"man:" + string + "\">" + string + "(" + sectionName + ")</a>";
            }
        }
        return "";
    }
    
    /** Returns true for C files, because only C programmers care about man pages. */
    public boolean isSuitable(ETextWindow textWindow) {
        return textWindow.isCPlusPlus();
    }
    
    /** Handles our non-standard "man:" scheme. */
    public boolean handleLink(String link) {
        if (link.startsWith("man:")) {
            String page = link.substring(4);
            try {
                new ShellCommand("man -S 2:3 " + page + " | col -b", ToolInputDisposition.NO_INPUT, ToolOutputDisposition.ERRORS_WINDOW).runCommand();
            } catch (Throwable th) {
                Evergreen.getInstance().showAlert("Couldn't show manual page", "There was a problem running man(1): " + th.getMessage() + ".");
            }
            return true;
        }
        return false;
    }
}
