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
        return formatManPage(string, "2:3");
    }
    
    /** Returns true for C files, because only C programmers care about man pages. */
    public boolean isSuitable(ETextWindow textWindow) {
        return textWindow.isCPlusPlus();
    }
    
    private String formatManPage(String page, String section) {
        // If we're on a system without rman(1), at least display something.
        String command = "man -S " + section + " " + page + " | col -b";
        
        String polyglotMan = findPolyglotMan();
        if (polyglotMan != null) {
            ArrayList<String> lines = new ArrayList<String>();
            ArrayList<String> errors = new ArrayList<String>();
            int status = ProcessUtilities.backQuote(null, ProcessUtilities.makeShellCommandArray("man -S " + section + " -w " + page), lines, errors);
            String filename = lines.get(0);
            if (FileUtilities.exists(filename) == false) {
                return "";
            }
            command = (filename.endsWith(".gz") ? "gunzip -c " : "cat ") + filename + " | rman -r 'man:%s(%s)' -S -f HTML";
        }
        
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<String> errors = new ArrayList<String>();
        int status = ProcessUtilities.backQuote(null, ProcessUtilities.makeShellCommandArray(command), lines, errors);
        
        String result = StringUtilities.join(lines, "\n");
        
        // Remove the table of contents at the end.
        result = result.replaceAll("(?s)<hr><p>.*?</ul>", "");
        // Remove the links to the table of contents.
        result = result.replaceAll("<a name='sect\\d+' href='#toc\\d+'>((.|\n)*?)</a>", "$1");
        result = result.replace("<a href='#toc'>Table of Contents</a><p>", "");
        
        return result;
    }
    
    /** Handles our non-IETF (but freedesktop.org-recommended) "man:" URI scheme. */
    public boolean handleLink(String link) {
        // We don't just test for the "man:" prefix because we're going to send the rest directly to a shell, so we don't want any nasty surprises.
        Matcher matcher = Pattern.compile("^man:([A-Za-z0-9]+)\\((\\d+)\\)$").matcher(link);
        if (matcher.matches()) {
            String page = matcher.group(1);
            String section = matcher.group(2);
            String manPage = formatManPage(page, section);
            if (manPage.length() > 0) {
                Advisor.getInstance().showDocumentation(manPage);
                return true;
            }
        }
        return false;
    }
    
    private String findPolyglotMan() {
        // "rman" is called "PolyglotMan" these days, and found at http://polyglotman.sourceforge.net/ but it retains its old name for the binary.
        // Debian Linux with the "rman" package has a binary in /usr/bin.
        // Mac OS with Xcode 2.3 or later installed has a binary in Xcode's plug-ins tree.
        // Mac OS with X11 installed has an older version in /usr/X11R6/bin.
        String[] possibilities = new String[] { "/usr/bin/rman", "/Library/Application Support/Apple/Developer Tools/Plug-ins/DocViewerPlugIn.xcplugin/Contents/Resources/rman", "/usr/X11R6/bin/rman" };
        for (String possibility : possibilities) {
            if (FileUtilities.exists(possibility)) {
                return possibility;
            }
        }
        return null;
    }
}
