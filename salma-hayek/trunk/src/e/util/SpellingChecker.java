package e.util;

import e.ptextarea.FileType;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Uses an ispell(1)-compatible back end to check spelling.
 */
public class SpellingChecker {
    private static final SpellingChecker instance = new SpellingChecker();
    private static final boolean DEBUGGING = false;
    
    private static final Stopwatch stopwatch = Stopwatch.get("SpellingChecker");
    
    private static WordCache wordCache = new WordCache();
    
    /**
     * Caches whether or not the last MAX_ENTRIES words were spelled correctly or incorrectly.
     */
    private static class WordCache extends LinkedHashMap<String, Boolean> {
        private static final int MAX_ENTRIES = 4096;
        
        public WordCache() {
            super(MAX_ENTRIES);
        }
        
        @Override protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_ENTRIES;
        }
    }
    
    private Process ispell;
    private PrintWriter out;
    private BufferedReader in;
    
    /** Returns the single instance of SpellingChecker. */
    public static synchronized SpellingChecker getSharedSpellingCheckerInstance() {
        return instance;
    }
    
    /** Establishes the connection to ispell, if possible. */
    private SpellingChecker() {
        // On Mac OS, we want to use the system's spelling checker, so try our NSSpell utility (which gives Apple's code an ispell-like interface) first.
        File nsSpellBinary = FileUtilities.findSupportBinary("NSSpell");
        if (nsSpellBinary != null && connectTo(new String[] { nsSpellBinary.toString(), "-a" })) {
            return;
        }
        // Otherwise try aspell(1) -- also used by gedit(1) -- first, and fall back to good old ispell(1).
        String[] backEnds = { "aspell", "ispell" };
        for (String backEnd : backEnds) {
            if (FileUtilities.findOnPath(backEnd) != null) {
                if (connectTo(new String[] { backEnd, "-a" })) {
                    return;
                }
            }
        }
        Log.warn("SpellingChecker: failed to find any back end. Please install aspell(1) or ispell(1).");
    }
    
    /** Attempts to connect to the given command-line spelling checker, which must be compatible with ispell's -a mode. */
    private boolean connectTo(String[] execArguments) {
        try {
            ispell = Runtime.getRuntime().exec(execArguments);
            in = new BufferedReader(new InputStreamReader(ispell.getInputStream()));
            out = new PrintWriter(ispell.getOutputStream());
            
            String greeting = in.readLine();
            if (greeting != null && greeting.startsWith("@(#) International Ispell ")) {
                Log.warn("SpellingChecker: connected to " + execArguments[0] + " okay: " + greeting + ".");
            } else {
                throw new IOException("Garbled ispell response: " + greeting);
            }
            out.println("!"); // Set terse mode.
            out.flush();
            return true;
        } catch (IOException ex) {
            Log.warn("SpellingChecker: couldn't start " + execArguments[0] + " (" + ex.getMessage() + "), though it was on the path.");
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
     * word cache is used to save on expensive inter-process communication.
     */
    public synchronized boolean isMisspelledWord(String word, FileType fileType) {
        if (ispell == null) {
            debug("ispell == null");
            return false;
        }
        
        word = word.toLowerCase();
        
        // Check the exceptions lists first...
        if (isException(word, fileType)) {
            return false;
        }
        
        // ...then the word cache...
        Boolean cachedResult = wordCache.get(word);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        // ...and only then give in and ask the spelling checker.
        // We copy the word into a new string to avoid accidental retention
        // of character arrays representing documents in their entirety.
        boolean misspelled = isMisspelledWordAccordingToIspell(word, null);
        wordCache.put(new String(word), Boolean.valueOf(misspelled));
        return misspelled;
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private static final HashMap<FileType, InheritingSet> SPELLING_EXCEPTIONS_MAP = new HashMap<FileType, InheritingSet>();
    
    // A set of strings whose 'contains' method delegates to its parent on failure.
    // This allows each FileType-specific set to delegate to a FileType-independent general purpose set.
    static class InheritingSet {
        private final HashSet<String> set = new HashSet<String>();
        private final InheritingSet parent;
        
        InheritingSet(InheritingSet parent) {
            this.parent = parent;
        }
        
        public synchronized void add(String word) {
            set.add(word);
        }
        
        public synchronized void addAll(Collection<String> words) {
            set.addAll(words);
        }
        
        public synchronized boolean contains(String word) {
            return set.contains(word) || (parent != null && parent.contains(word));
        }
    }
    
    // The current top of the delegation chain is this set of FileType-independent exceptions.
    private static final InheritingSet GENERAL_PURPOSE_EXCEPTIONS = getGeneralPurposeExceptions();
    
    /**
     * Tests whether the text component we're checking declares the given word
     * as a spelling exception in its language.
     */
    private boolean isException(String word, FileType fileType) {
        return getExceptionsFor(fileType).contains(word);
    }
    
    private InheritingSet getExceptionsFor(FileType fileType) {
        InheritingSet exceptions;
        synchronized (SPELLING_EXCEPTIONS_MAP) {
            exceptions = SPELLING_EXCEPTIONS_MAP.get(fileType);
        }
        if (exceptions == null) {
            exceptions = initSpellingExceptionsFor(fileType);
            synchronized (SPELLING_EXCEPTIONS_MAP) {
                SPELLING_EXCEPTIONS_MAP.put(fileType, exceptions);
            }
        }
        return exceptions;
    }
    
    // Used by Evergreen's Advisors to supply extra exceptions they've gleaned at runtime.
    public void addSpellingExceptionsFor(FileType fileType, Set<String> extraExceptions) {
        getExceptionsFor(fileType).addAll(extraExceptions);
    }
    
    private static InheritingSet initSpellingExceptionsFor(FileType fileType) {
        InheritingSet result = new InheritingSet(GENERAL_PURPOSE_EXCEPTIONS);
        
        // None of the language's keywords should be considered misspelled.
        result.addAll(Arrays.asList(fileType.getKeywords()));
        
        // And there may be a file of extra spelling exceptions for this language.
        readSpellingExceptionsFile(getSpellingExceptionsFilename("spelling-exceptions-" + fileType.getName()), result);
        
        return result;
    }
    
    private static InheritingSet getGeneralPurposeExceptions() {
        InheritingSet result = new InheritingSet(null);
        readSpellingExceptionsFile(getSpellingExceptionsFilename("spelling-exceptions"), result);
        return result;
    }
    
    private static String getSpellingExceptionsFilename(String name) {
        return System.getProperty("org.jessies.supportRoot") + File.separator + "lib" + File.separator + "data" + File.separator + name;
    }
    
    private static void readSpellingExceptionsFile(String filename, InheritingSet result) {
        if (!FileUtilities.exists(filename)) {
            return;
        }
        for (String exception : StringUtilities.readLinesFromFile(filename)) {
            if (exception.startsWith("#")) {
                continue; // Ignore comments.
            }
            result.add(exception);
        }
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
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
    public synchronized void acceptSpelling(String word, FileType fileType) {
        if (isMisspelledWord(word, fileType) == false) {
            return;
        }
        
        // The word cache only contains lowercase words.
        wordCache.remove(word.toLowerCase());
        
        // Send the word to ispell to insert into the personal dictionary.
        // FIXME: we pass it through with its original case, but if it's not all lowercase, ispell(1) takes that to mean that it should only accept that capitalization. This may not be the right choice.
        out.println("*" + word);
        out.flush();
    }
    
    private boolean isMisspelledWordAccordingToIspell(String word, Collection<String> returnSuggestions) {
        Stopwatch.Timer timer = stopwatch.start();
        try {
            // Send the word to ispell for checking.
            String request = "^" + word;
            debug(request);
            out.println(request);
            out.flush();
            
            // ispell's response will be one of:
            // 1. a blank line (meaning "correctly spelled"),
            // 2. lines beginning with [&?#] containing suggested corrections, followed by a blank line.
            String response = in.readLine();
            if (response == null) {
                Log.warn("SpellingChecker: lost connection to back end.");
                return false;
            }
            
            // A blank line means "correctly spelled".
            if (response.length() == 0) {
                debug("\"" + word + "\" response length == 0");
                return false;
            }
            
            // &: near-miss
            // ?: guess
            // #: no suggestions
            boolean misspelled = true;
            while (response != null && response.length() > 0 && "&?#+-".indexOf(response.charAt(0)) != -1) {
                debug(" " + response);
                
                if (response.charAt(0) == '&' && isCorrectIgnoringCase(word, response)) {
                    misspelled = false;
                }
                
                if (returnSuggestions != null) {
                    fillCollectionWithSuggestions(response, returnSuggestions);
                }
                
                response = in.readLine();
            }
            
            if (response != null && response.length() != 0) {
                Log.warn("SpellingChecker: garbled response: \"" + response + "\"");
            }
            
            return misspelled;
        } catch (IOException ex) {
            // What do we know, other than we failed to get an answer?
            // Should we stop talking to ispell?
            Log.warn("SpellingChecker: I/O error.", ex);
            return false;
        } finally {
            timer.stop();
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
        for (String suggestion : suggestions) {
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
