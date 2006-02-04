package e.util;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Uses ispell(1) to check spelling.
 */
public class SpellingChecker {
    private static final SpellingChecker instance = new SpellingChecker();
    private static final boolean DEBUGGING = false;
    
    private static HashSet<String> knownGood = new HashSet<String>();
    private static HashSet<String> knownBad = new HashSet<String>();
    
    private Process ispell;
    private PrintWriter out;
    private BufferedReader in;
    
    /** Returns the single instance of SpellingChecker. */
    public static synchronized SpellingChecker getSharedSpellingCheckerInstance() {
        return instance;
    }
    
    /** Establishes the connection to ispell, if possible. */
    private SpellingChecker() {
        boolean found = false;
        
        if (GuiUtilities.isMacOs()) {
            // On Mac OS, we want to use the system's spelling checker, so try
            // our NSSpell utility (which gives Apple's code an ispell-like
            // interface) first. We try to find it relative to this class'
            // location. This will break if the "classes" directory is moved
            // away from the "native" directory, or if the "native" directory's
            // contents are re-arranged.
            
            // Why commit such a potentially fragile hack? Well, the breakage
            // will be easy to spot, and unlikely, and in the meantime it
            // means we can offer system spelling checking on Mac OS with
            // no installation necessary.
            
            String nsspellPath = FileUtilities.getSalmaHayekFile("/.generated/native/Darwin/NSSpell/Darwin/NSSpell").toString();
            found = connectTo(new String[] { nsspellPath });
        }
        
        if (found == false) {
            connectTo(new String[] { "ispell", "-a" });
        }
    }
    
    /** Attempts to connect to the given command-line spelling checker, which must be compatible with ispell's -a mode. */
    private boolean connectTo(String[] execArguments) {
        try {
            ispell = Runtime.getRuntime().exec(execArguments);
            in = new BufferedReader(new InputStreamReader(ispell.getInputStream()));
            out = new PrintWriter(ispell.getOutputStream());
            
            String greeting = in.readLine();
            if (greeting.startsWith("@(#) International Ispell ")) {
                Log.warn("Connected to " + execArguments[0] + " okay.");
            } else {
                throw new IOException("Garbled ispell response: " + greeting);
            }
            out.println("!"); // Set terse mode.
            out.flush();
            return true;
        } catch (IOException ex) {
            Log.warn("Couldn't start " + execArguments[0] + " (" + ex.getMessage() + ") assuming it isn't installed.");
            ispell = null;
            in = null;
            out = null;
            return false;
        }
    }
    
    /**
     * Tests whether the given word is misspelled.
     * If ispell is unavailable, no words are considered misspelled.
     * We only ask ispell about any given word at most once: the
     * knownGood and knownBad HashSets are used to save on
     * expensive inter-process communication.
     */
    public synchronized boolean isMisspelledWord(String word) {
        if (ispell == null) {
            debug("ispell == null");
            return false;
        }
        
        word = word.toLowerCase();
        
        // Check the known-good words first, because good words should be more common.
        if (knownGood.contains(word)) {
            return false;
        }
        // Then check the known-bad words.
        if (knownBad.contains(word)) {
            return true;
        }
        // Then give in and ask ispell.
        boolean misspelled = isMisspelledWordAccordingToIspell(word, null);
        
        // Ensure that this word makes its way into one set or the other.
        // We copy the word into a new string to avoid accidental retention
        // of character arrays representing documents in their entirety.
        (misspelled ? knownBad : knownGood).add(new String(word));
        return misspelled;
    }
    
    public synchronized String[] getSuggestionsFor(String misspelledWord) {
        ArrayList<String> suggestions = new ArrayList<String>();
        boolean isMisspelled = isMisspelledWordAccordingToIspell(misspelledWord, suggestions);
        if (isMisspelled == false) {
            return new String[0];
        }
        return suggestions.toArray(new String[suggestions.size()]);
    }
    
    /**
     * Moves the word from the known bad set to the known good set,
     * and inserts it into the user's personal ispell dictionary.
     */
    public synchronized void acceptSpelling(String word) {
        if (isMisspelledWord(word) == false) {
            return;
        }
        
        // knownBad and knownGood only contain lowercase words.
        String setWord = word.toLowerCase();
        knownBad.remove(setWord);
        knownGood.add(setWord);
        
        // Send the word to ispell to insert into the personal dictionary.
        // FIXME: we pass it through with its original case, but if it's not all lowercase, ispell(1) takes that to mean that it should only accept that capitalization. This may not be the right choice.
        out.println("*" + word);
        out.flush();
    }
    
    public static synchronized void dumpKnownBadWordsTo(PrintStream out) {
        // Get a sorted list of the known bad words.
        Iterator it = knownBad.iterator();
        ArrayList<String> words = new ArrayList<String>();
        for (String word : knownBad) {
            words.add(word);
        }
        Collections.sort(words);
        
        // Dump them.
        out.println("SpellingChecker's set of known-bad words:");
        for (int i = 0; i < words.size(); i++) {
            out.println(words.get(i));
        }
        out.println("=" + words.size());
    }
    
    private boolean isMisspelledWordAccordingToIspell(String word, Collection<String> returnSuggestions) {
        // Send the word to ispell for checking.
        String request = "^" + word;
        debug(request);
        out.println(request);
        out.flush();
        
        // ispell's response will be one of:
        // 1. a blank line (meaning "correctly spelled"),
        // 2. lines beginning with [&?#] containing suggested corrections, followed by a blank line.
        try {
            String response = in.readLine();
            
            // A blank line means "correctly spelled".
            if (response.length() == 0) {
                debug("'" + word + "' response length == 0");
                return false;
            }
            
            // &: near-miss
            // ?: guess
            // #: no suggestions
            boolean misspelled = true;
            while (response.length() > 0 && "&?#+-".indexOf(response.charAt(0)) != -1) {
                debug(" " + response);
                
                if (response.charAt(0) == '&' && isCorrectIgnoringCase(word, response)) {
                    misspelled = false;
                }
                
                if (returnSuggestions != null) {
                    fillCollectionWithSuggestions(response, returnSuggestions);
                }
                
                response = in.readLine();
            }
            
            if (response.length() != 0) {
                Log.warn("ispell: garbled response: '" + response + "'");
            }
            
            return misspelled;
        } catch (IOException ex) {
            // What do we know, other than we failed to get an answer?
            // Should we stop talking to ispell?
            ex.printStackTrace();
            return false;
        }
    }
    
    /**
     * Tests whether a spelling would be correct if we didn't care about case.
     * In code, case is often dependent on naming conventions rather than
     * linguistics. So 'british' might be a reasonable identifier, and we might
     * say 'isAscii' instead of 'isASCII'.
     */
    private boolean isCorrectIgnoringCase(String word, String response) {
        /*
         * From the ispell(1) man page:
         * 
         * If the word is not in the dictionary, but there are near  misses,  then
         * the  line  contains  an '&', a space, the misspelled word, a space, the
         * number of near misses, the number of characters between  the  beginning
         * of  the line and the beginning of the misspelled word, a colon, another
         * space, and a list of the near misses separated by  commas  and  spaces.
         * Following  the  near  misses  (and identified only by the count of near
         * misses), if the word could be formed by adding (illegal) affixes  to  a
         * known root, is a list of suggested derivations, again separated by com-
         * mas and spaces.
         */
        Pattern pattern = Pattern.compile("^& .* \\d+ \\d+: ([^,]+)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find() == false) {
            return false; // We don't understand what ispell's said, so let's not assume anything.
        }
        List<String> suggestions = new ArrayList<String>();
        fillCollectionWithSuggestions(response, suggestions);
        for (Iterator i = suggestions.iterator(); i.hasNext(); ) {
            String suggestion = (String) i.next();
            if (suggestion.equalsIgnoreCase(word)) {
                return true;
            }
        }
        return false;
    }
    
    private String[] extractSuggestions(String response) {
        // Does this response actually have any suggestions?
        if ("&?".indexOf(response.charAt(0)) == -1) {
            return new String[0];
        }
        
        return response.replaceFirst("^[&\\?] .* \\d+ \\d+: ", "").split(", ");
    }
    
    private void fillCollectionWithSuggestions(String response, Collection<String> returnSuggestions) {
        String[] suggestions = extractSuggestions(response);
        for (String suggestion : suggestions) {
            returnSuggestions.add(suggestion);
        }
    }
    
    private void debug(String message) {
        if (DEBUGGING) {
            Log.warn("SpellingChecker: " + message);
        }
    }
}
