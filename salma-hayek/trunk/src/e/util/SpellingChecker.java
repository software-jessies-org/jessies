package e.util;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Uses aspell(1) or ispell(1) to check spelling.
 */
public class SpellingChecker {
    private Process ispell;
    private PrintWriter out;
    private BufferedReader in;
    private BufferedReader err;
    
    private static HashSet knownGood = new HashSet();
    private static HashSet knownBad = new HashSet();
    
    private static SpellingChecker instance;
    
    /** Returns the single instance of SpellingChecker. */
    public static synchronized SpellingChecker getSharedSpellingCheckerInstance() {
        if (instance == null) {
            instance = new SpellingChecker();
        }
        return instance;
    }
    
    /** Establishes the connection to aspell or ispell, if possible. We favor ispell because it's faster. */
    private SpellingChecker() {
        boolean found = connectTo(new String[] { "ispell", "-a" });
        if (found == false) {
            connectTo(new String[] { "aspell", "-a" });
        }
    }
    
    /** Attempts to connect to the given command-line spelling checker, which must be compatible with ispell's -a mode. */
    private boolean connectTo(String[] execArguments) {
        try {
            ispell = Runtime.getRuntime().exec(execArguments);
            in = new BufferedReader(new InputStreamReader(ispell.getInputStream()));
            out = new PrintWriter(ispell.getOutputStream());
            
            /*
            err = new BufferedReader(new InputStreamReader(ispell.getErrorStream()));
            new Thread("ISpell Error Reporter") {
                public void run() {
                    try {
                        String line;
                        while ((line = err.readLine()) != null) {
                            System.err.println("ispell: " + line);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } finally {
                    }
                }
            }.start();
            */
            
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
        ArrayList suggestions = new ArrayList();
        boolean isMisspelled = isMisspelledWordAccordingToIspell(misspelledWord, suggestions);
        if (isMisspelled == false) {
            return new String[0];
        }
        return (String[]) suggestions.toArray(new String[suggestions.size()]);
    }
    
    public static synchronized void dumpKnownBadWordsTo(PrintStream out) {
        // Get a sorted list of the known bad words.
        Iterator it = knownBad.iterator();
        ArrayList words = new ArrayList();
        while (it.hasNext()) {
            words.add(it.next());
        }
        Collections.sort(words);
        
        // Dump them.
        out.println("SpellingChecker's set of known-bad words:");
        for (int i = 0; i < words.size(); i++) {
            out.println(words.get(i));
        }
        out.println("=" + words.size());
    }
    
    private boolean isMisspelledWordAccordingToIspell(String word, Collection returnSuggestions) {
        // Send the word to ispell for checking.
        String request = "^" + word;
        //System.err.println(request);
        out.println(request);
        out.flush();
        
        // ispell's response will be one of:
        // 1. a blank line (meaning "correctly spelled"),
        // 2. lines beginning with [&?#] containing suggested corrections, followed by a blank line.
        try {
            String response = in.readLine();
            
            // A blank line means "correctly spelled".
            if (response.length() == 0) {
                return false;
            }
            
            // &: near-miss
            // ?: guess
            // #: no suggestions
            boolean misspelled = true;
            while (response.length() > 0 && "&?#+-".indexOf(response.charAt(0)) != -1) {
                //System.err.println(" " + response);
                
                if (response.charAt(0) == '&' && isCorrectIgnoringCase(word, response)) {
                    misspelled = false;
                }
                
                if (returnSuggestions != null) {
                    fillCollectionWithSuggestions(response, returnSuggestions);
                }
                
                response = in.readLine();
            }
            
            if (response.length() != 0) {
                System.err.println("ispell: garbled response: '" + response + "'");
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
        String firstSuggestion = matcher.group(1).trim();
        boolean result = firstSuggestion.equalsIgnoreCase(word);
        return result;
    }
    
    private String[] extractSuggestions(String response) {
        // Does this response actually have any suggestions?
        if ("&?".indexOf(response.charAt(0)) == -1) {
            return new String[0];
        }
        
        return response.replaceFirst("^[&\\?] .* \\d+ \\d+: ", "").split(", ");
    }
    
    private void fillCollectionWithSuggestions(String response, Collection returnSuggestions) {
        String[] suggestions = extractSuggestions(response);
        for (int i = 0; i < suggestions.length; i++) {
            returnSuggestions.add(suggestions[i]);
        }
    }
}
