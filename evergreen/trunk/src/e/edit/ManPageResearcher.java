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
    // A set of unique man page names, so we can quickly determine whether we have a suitable page.
    // This also lets us avoid accidentally returning man pages we're trying to ignore.
    private static final Set<String> uniqueManPageNames = new TreeSet<String>();
    // A set of unique words, for the spelling checker.
    private static final Set<String> uniqueWords = new TreeSet<String>();
    
    private static final ManPageResearcher INSTANCE = new ManPageResearcher();
    
    private ManPageResearcher() {
        init();
    }
    
    public synchronized static ManPageResearcher getSharedInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the set of known man pages.
     */
    private static void init() {
        final long t0 = System.nanoTime();
        
        int pageCount = 0;
        Pattern manPagePattern = Pattern.compile("^(.*)\\.([23][A-Za-z]*)(\\.gz)?$");
        for (File manPath : findManPageDirectories()) {
            String[] manPages = manPath.list();
            if (manPages == null) manPages = new String[0];
            for (String manPage : manPages) {
                Matcher matcher = manPagePattern.matcher(manPage);
                if (matcher.find()) {
                    String stub = matcher.group(1);
                    String sectionName = matcher.group(2);
                    // FIXME: have we ever seen a useful man page whose section name wasn't wholly numeric? Maybe we should have a list of those non-numeric sections we *do* want, instead?
                    if (sectionName.endsWith("blt") || sectionName.endsWith("perl") || sectionName.endsWith("pm") || sectionName.endsWith("ssl") || sectionName.endsWith("tcl")) {
                        continue;
                    }
                    uniqueManPageNames.add(stub);
                    ++pageCount;
                } else {
                    Log.warn("unexpected man page \"" + manPage + "\"");
                }
            }
        }
        
        Advisor.extractUniqueWords(uniqueManPageNames, uniqueWords);
        
        final long t1 = System.nanoTime();
        Log.warn("Learned of " + pageCount + " man pages in " + TimeUtilities.nsToString(t1 - t0) + ".");
    }
    
    private static List<File> findManPageDirectories() {
        ArrayList<File> manPaths = new ArrayList<File>();
        Pattern manPathPattern = Pattern.compile("^(?:MANDATORY_)?MANPATH\\s+(.*)$");
        String[] configurationPossibilities = new String[] { "/usr/share/misc/man.conf", "/etc/manpath.config" };
        for (String configurationPossibility : configurationPossibilities) {
            if (FileUtilities.exists(configurationPossibility)) {
                for (String line : StringUtilities.readLinesFromFile(configurationPossibility)) {
                    Matcher matcher = manPathPattern.matcher(line);
                    if (matcher.find()) {
                        manPaths.add(new File(matcher.group(1), "man2"));
                        manPaths.add(new File(matcher.group(1), "man3"));
                    }
                }
            }
        }
        return manPaths;
    }
    
    public String research(String string) {
        if (uniqueManPageNames.contains(string) == false) {
            return "";
        }
        return formatManPage(string, "2:3");
    }
    
    /** Returns true for C files, because only C programmers care about man pages. */
    public boolean isSuitable(FileType fileType) {
        return fileType == FileType.C_PLUS_PLUS;
    }
    
    // FIXME: we're relying on unmaintained code (rman) here, and should really look at doing something better.
    // If nothing else, we should pull this out into a helper script. We're not gaining anything by writing this in Java.
    public String formatManPage(String page, String section) {
        ArrayList<String> commands = new ArrayList<String>();
        String polyglotMan = findPolyglotMan();
        if (polyglotMan == null) {
            // If we're on a system without rman(1), at least display something.
            commands.add("man -a -S " + section + " " + page + " | col -b");
        } else {
            polyglotMan = "'" + polyglotMan + "' -f HTML -r 'man:%s(%s)'";
            
            ArrayList<String> lines = new ArrayList<String>();
            ArrayList<String> errors = new ArrayList<String>();
            int status = ProcessUtilities.backQuote(null, ProcessUtilities.makeShellCommandArray("man -a -S " + section + " -w " + page), lines, errors);
            if (status != 0) {
                return "";
            }
            
            for (String filename : lines) {
                if (FileUtilities.exists(filename) == false) {
                    continue;
                }
                if (GuiUtilities.isMacOs()) {
                    // On Mac OS, the ancient version of rman(1) only seems to work with pre-formatted pages, and then only if you let it guess.
                    commands.add("nroff -man '" + filename + "' | " + polyglotMan);
                } else {
                    // On Linux, modern rman(1) works best with man page source, and then only if you explicitly tell it that's what it's got.
                    commands.add((filename.endsWith(".gz") ? "gunzip -c" : "cat") + " '" + filename + "' | " + polyglotMan + " -S ");
                }
            }
        }
        
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<String> errors = new ArrayList<String>();
        for (String command : commands) {
            // FIXME: should we do anything if status is non-zero?
            int status = ProcessUtilities.backQuote(null, ProcessUtilities.makeShellCommandArray(command), lines, errors);
        }
        
        String result = StringUtilities.join(lines, "\n");
        
        if (polyglotMan != null) {
            // Tidy up the PolyglotMan (rman) output.
            
            // There are case differences between 3.0.9 on Mac OS and 3.2 on Linux.
            // There's also a difference in the quote character used: ' on Linux and " on Mac OS.
            
            // Remove the table of contents at the end.
            // There may be nested lists because subsections are included, so we really do want a greedy match.
            // We add the "</body>" because sometimes we get multiple concatenated pages, and we don't want to skip all but the first.
            result = result.replaceAll("(?si)<hr><p>.*?</ul>\n</body>", "");
            
            // Try to make the synopses look better by insisting that they're in PREs, and doing a few other bits of tidying.
            // Good test pages for this code: exec fcntl getenv getrusage pread setpgid stat.
            result = new Rewriter("(?si)(Synopsis</a></h2>\n)(.*?)(<h2>)") {
                public String replacement() {
                    String synopsis = group(2);
                    
                    // Remove custom formatting; being in a PRE is more readable than all the bold and italic in the world.
                    synopsis = synopsis.replaceAll("</?(b|br|i|p|pre)>", "");
                    
                    // Turn fancy spaces back into ASCII spaces, and stuff magically lines back up again.
                    synopsis = synopsis.replaceAll("&nbsp;", " ");
                    
                    // Cope with the "feature test macro requirements" that arrived in glibc manual pages recently.
                    // We want the heading to be body text, but the details back in a PRE.
                    // FIXME: we (via rman) do a terrible job of formatting this stuff.
                    synopsis = synopsis.replaceAll("(?si)(Feature Test Macro.*?</a>\n\\):)", "</pre>$1<pre>");
                    
                    // Re-join stuff like "size_t\npread(..." which isn't helpful.
                    synopsis = synopsis.replaceAll("\n", "").replaceAll(" +", " "); // Remove ASCII art.
                    synopsis = synopsis.replaceAll("#", "\n#"); // Break before any #define or #include.
                    synopsis = synopsis.replaceAll("&gt;([^#])", "&gt;\n$1"); // Break at the end the last #include.
                    synopsis = synopsis.replaceAll("(?<!lt);", ";\n"); // Break after any ; that ends a declaration.
                    
                    // One last pass tidying whitespace.
                    String[] lines = synopsis.split("\n");
                    for (int i = 0; i < lines.length; ++i) {
                        lines[i] = lines[i].trim();
                    }
                    synopsis = StringUtilities.join(lines, "\n");
                    
                    return group(1) + "<pre>" + synopsis + "</pre>" + group(3);
                }
            }.rewrite(result);
            
            // Remove the links to the table of contents.
            result = result.replaceAll("(?i)<a name=['\"]sect\\d+['\"] href=['\"]#toc\\d+['\"]>((.|\n)*?)</a>", "$1");
            result = result.replaceAll("(?i)<a href=['\"]#toc['\"]>Table of Contents</a><p>", "");
        } else {
            result = "\n\n(You can get a nicely-formatted hyperlinked man page by installing PolyglotMan, also known as rman. You can get the source from http://polyglotman.sourceforge.net/ if you can't find it prepackaged for your platform.)\n\n\n" + result;
            result = result.replaceAll("\n", "<br>\n");
        }
        
        return result;
    }
    
    /** Handles our non-IETF (but freedesktop.org-recommended) "man:" URI scheme. */
    public boolean handleLink(String link) {
        // We don't just test for the "man:" prefix because we're going to send the rest directly to a shell, so we don't want any nasty surprises.
        Matcher matcher = Pattern.compile("^man:([A-Za-z0-9]+)\\((\\d+)\\)$").matcher(link);
        if (matcher.find()) {
            String page = matcher.group(1);
            String section = matcher.group(2);
            String manPage = formatManPage(page, section);
            if (manPage.length() > 0) {
                Advisor.getInstance().setDocumentationText(manPage);
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
    
    /**
     * Adds all the words used in identifiers that have man pages, for C++.
     */
    public void addWordsTo(Set<String> words) {
        words.addAll(uniqueWords);
    }
}
