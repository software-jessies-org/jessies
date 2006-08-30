package e.edit;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import e.ptextarea.*;
import e.util.*;

/**
 * Offers man page entries corresponding to selected words.
 */
public class StlDocumentationResearcher implements WorkspaceResearcher {
    private static HashMap<String, String> docs;
    private static TreeSet<String> uniqueWords;
    private static final String STL_DOCUMENTATION_ROOT = "/usr/share/doc/stl-manual/html/";
    
    public StlDocumentationResearcher() {
        final long startTime = System.currentTimeMillis();
        
        docs = new HashMap<String, String>();
        
        TreeSet<String> uniqueIdentifiers = new TreeSet<String>();
        
        if (FileUtilities.exists(STL_DOCUMENTATION_ROOT) == false) {
            return;
        }
        
        Pattern pattern = Pattern.compile(".*<A href=\"(.+\\.html)\">([a-z0-9_]+)(&lt;.*&gt;)?</A></TD>.*");
        String[] lines = StringUtilities.readLinesFromFile(STL_DOCUMENTATION_ROOT + "stl_index.html");
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                String filename = STL_DOCUMENTATION_ROOT + matcher.group(1);
                String term = matcher.group(2);
                docs.put(term, filename);
                
                // std::string and std::wstring are typedefs for std::basic_string.
                if (term.equals("basic_string")) {
                    docs.put("string", filename);
                    docs.put("wstring", filename);
                }
                
                uniqueIdentifiers.add(term);
            }
        }
        
        // FIXME: this turns "posix_openpt" into two words, so the spelling checker will accept "openpt" alone, rather than just in the identifier "posix_openpt" as intended. Maybe we should check blessed identifiers as a whole before we try break them into words, and then supply the spelling checker with the unique identifiers we bless, rather than just the list of words? (At the same time, passing all the words works well for Java source.)
        uniqueWords = JavaResearcher.extractUniqueWords(uniqueIdentifiers.iterator());
        
        final long duration = System.currentTimeMillis() - startTime;
        Log.warn("Learned of " + docs.size() + " STL terms in " + duration + "ms.");
    }
    
    // FIXME: add STL words to C++ files!
    public void addManPageWordsTo(Set<String> set) {
        set.addAll(uniqueWords);
    }
    
    public String research(String string) {
        return getDocumentation(string);
    }
    
    /** Returns true for C++ files, because only C++ programmers care about STL documentation. */
    public boolean isSuitable(ETextWindow textWindow) {
        return textWindow.getFileType() == FileType.C_PLUS_PLUS;
    }
    
    private String getDocumentation(String term) {
        if (term.startsWith("std::") == false) {
            return null;
        }
        
        // Skip "std::".
        term = term.substring(5);
        
        // Return the HTML if we have it. The STL documentation is simple enough for us to render it ourselves.
        String filename = docs.get(term);
        String result = null;
        if (filename != null) {
            result = StringUtilities.readFile(filename);
            // Fix relative links.
            result = result.replaceAll("(?i)<head>", "<head><base href=\"file://" + STL_DOCUMENTATION_ROOT + "\">");
            // Tidy up the member tables by making all TT-only TD elements use PRE instead.
            result = result.replaceAll("(?i)\n<tt>(.*?)</tt>\n</td>", "\n<pre>$1</pre></td>\n");
        }
        return result;
    }
    
    /** Handles our non-IETF "stl:" URI scheme. */
    public boolean handleLink(String link) {
        Matcher matcher = Pattern.compile("^stl:([A-Za-z0-9_]+)$").matcher(link);
        if (matcher.matches()) {
            String term = matcher.group(1);
            if (term.startsWith("std::") == false) {
                term = "std::" + term;
            }
            String html = getDocumentation(term);
            if (html.length() > 0) {
                Advisor.getInstance().showDocumentation(html);
                return true;
            }
        }
        return false;
    }
}
