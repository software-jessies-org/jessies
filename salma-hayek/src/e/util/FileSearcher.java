package e.util;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public class FileSearcher {
    private final Pattern pattern;
    
    /** Creates a new FileSearcher for finding the given Pattern. */
    public FileSearcher(Pattern pattern) {
        this.pattern = pattern;
    }
    
    /**
     * Search for occurrences of the input pattern in the given file.
     * Returns false if unable to search; true otherwise.
     */
    public boolean searchFile(Path file, Collection<String> matches) throws IOException {
        CountingMatcher matcher = new CountingMatcher(matches);
        try (Stream<String> stream = Files.lines(file, StandardCharsets.UTF_8)) {
            stream.forEach(v -> matcher.tryMatch(v));
        } catch (UncheckedIOException ex) {
            // These happen when there's some error in decoding the charset. We'll treat this as being
            // a sign that the file is a binary file.
            return false;
        }
        return true;
    }
    
    /**
     * CountingMatcher matches a sequence of lines, appending any matches to the 'matches' collection.
     * This is for use with a streamed line reader.
     */
    public class CountingMatcher {
        private int line = 0;
        private Collection<String> matches;
        
        public CountingMatcher(Collection<String> matches) {
            this.matches = matches;
        }
        
        public void tryMatch(String str) {
            line++;
            if (pattern.matcher(str).find()) {
                matches.add(":" + line + ":" + str);
            }
        }
    }
}
