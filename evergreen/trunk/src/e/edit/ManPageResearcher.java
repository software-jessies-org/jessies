package e.edit;

import java.util.*;
import e.ptextarea.*;
import e.util.*;

/**

Offers man page entries corresponding to selected words.

 */
public class ManPageResearcher implements WorkspaceResearcher {
    /** Keep the names of all the known man pages. */
    private Set<String> knownManPages;
    
    /**
     * Initializes the set of known man pages.
     */
    public ManPageResearcher() {
        final long startTime = System.currentTimeMillis();
        knownManPages = new HashSet<String>();
        
        /*
         * Should really do something equivalent to this:
         * locate man/man | grep "man[23]$" | xargs find | perl -ne 'if (m/\/([^\/.]*)\..*$/) { print "$1\n"; }' | sort | wc -l
         *
         * Probably starting by looking in /etc/manpath.config instead of using locate.
         * But why write a hundred lines of Java when a single line of shell will do?
         * 
         * For now, we'll assume that we have a file listing all the interesting man pages.
         * This has the advantage that you can remove any entries that annoy you.
         */
        
        String[] manPages = StringUtilities.readLinesFromFile(Edit.getInstance().getResourceFilename("manpages"));
        for (String manPage : manPages) {
            knownManPages.add(manPage);
        }
        
        final long duration = System.currentTimeMillis() - startTime;
        Log.warn("Learned of " + knownManPages.size() + " man pages in " + duration + "ms.");
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
                new ShellCommand("man -a " + page + " | col -b").runCommand();
            } catch (Throwable th) {
                Edit.getInstance().showAlert("Man Page", "Can't run man(1) (" + th.getMessage() + ").");
            }
            return true;
        }
        return false;
    }
}
