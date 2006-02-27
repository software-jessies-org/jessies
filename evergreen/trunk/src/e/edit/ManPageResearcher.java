package e.edit;

import java.util.*;
import e.ptextarea.*;
import e.util.*;

/**

Offers man page entries corresponding to selected words.

 */
public class ManPageResearcher implements WorkspaceResearcher {
    /** Keep the names of all the known man pages. */
    private static Set<String> knownManPages;
    
    private static TreeSet<String> uniqueIdentifiers;
    private static TreeSet<String> uniqueWords;
    
    private static ManPageResearcher INSTANCE = new ManPageResearcher();
    
    private ManPageResearcher() {
    }
    
    public synchronized static ManPageResearcher getSharedInstance() {
        if (knownManPages == null) {
            init();
        }
        return INSTANCE;
    }

    /**
     * Initializes the set of known man pages.
     */
    private static void init() {
        final long startTime = System.currentTimeMillis();
        knownManPages = new HashSet<String>();
        
        /*
         * Should really do something equivalent to this:
         * 
         * locate man/man | grep -v "^/Previous Systems/" | grep "/man[23]$" |  tr '\n' '\0' | xargs -0 find | grep -Ev "\.3(pm|ssl|tcl)$" | perl -ne 'if (m/\/([^\/.]*)\..*$/) { print "$1\n"; }' | sort -u
         * 
         * What to do on systems without locate(1), or where the database hasn't been generated?
         * 
         * For now, we'll assume that we have a file listing all the interesting man pages.
         * This has the advantage that you can remove any entries that annoy you.
         */
        
        String[] manPages = StringUtilities.readLinesFromFile(Edit.getInstance().getResourceFilename("manpages"));
        uniqueIdentifiers = new TreeSet<String>();
        for (String manPage : manPages) {
            knownManPages.add(manPage);
            uniqueIdentifiers.add(manPage);
        }
        
        // FIXME: this turns "posix_openpt" into two words, so the spelling checker will accept "openpt" alone, rather than just in the identifier "posix_openpt" as intended. Maybe we should check blessed identifiers as a whole before we try break them into words, and then supply the spelling checker with the unique identifiers we bless, rather than just the list of words? (At the same time, passing all the words works well for Java source.)
        uniqueWords = JavaResearcher.extractUniqueWords(uniqueIdentifiers.iterator());
        
        final long duration = System.currentTimeMillis() - startTime;
        Log.warn("Learned of " + knownManPages.size() + " man pages in " + duration + "ms.");
    }
    
    public synchronized static void addManPageWordsTo(Set<String> set) {
        set.addAll(uniqueWords);
    }
    
    /**
     * Look for something in a JTextComponent. Returns an HTML string
     * containing information about what it found. Should return
     * the empty string (not null) if it has nothing to say.
     */
    public String research(PTextArea text, String string) {
        if (knownManPages.contains(string)) {
            return "Manual page for <a href=\"man:" + string + "\">" + string + "</a>";
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
                new ShellCommand("man -S 2:3 " + page + " | col -b").runCommand();
            } catch (Throwable th) {
                Edit.getInstance().showAlert("Man Page", "Can't run man(1) (" + th.getMessage() + ").");
            }
            return true;
        }
        return false;
    }
}
