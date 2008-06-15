package e.util;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.regex.*;

public class FileSearcher {
    private Pattern pattern;
    
    /** Creates a new FileSearcher for finding the given Pattern. */
    public FileSearcher(Pattern pattern) {
        this.pattern = pattern;
    }
    
    /** Finds the index of the next newline character in 'charSequence' after 'start'. */
    private int findEndOfLine(CharSequence charSequence, int start) {
        final int max = charSequence.length();
        for (int i = start; i < max; i++) {
            if (charSequence.charAt(i) == '\n') {
                return i;
            }
        }
        return max - 1;
    }
    
    /**
     * Use the linePattern to break the given CharBuffer into lines, applying
     * the input pattern to each line to see if we have a match.
     */
    private void searchCharBuffer(CharSequence charSequence, Collection<String> matches) {
        // Early exit on non-matching files.
        // Making use of the match location to optimize the loop below didn't show any significant improvement, despite doubling the amount of code.
        Matcher firstMatch = pattern.matcher(charSequence);
        if (firstMatch.find() == false) {
            return;
        }
        
        Matcher patternMatcher = pattern.matcher("");
        int start = 0;
        for (int lineNumber = 1; start < charSequence.length(); lineNumber++) {
            int end = findEndOfLine(charSequence, start);
            CharSequence currentLine = charSequence.subSequence(start, end);
            patternMatcher.reset(currentLine);
            if (patternMatcher.find()) {
                matches.add(":" + lineNumber + ":" + currentLine);
            }
            start = end + 1;
        }
    }
    
    /**
     * Search for occurrences of the input pattern in the given file.
     * Returns false if unable to search; true otherwise.
     */
    public boolean searchFile(File file, Collection<String> matches) throws IOException {
        int byteCount = (int) file.length();
        ByteBuffer byteBuffer = ByteBufferUtilities.readFile(file);
        
        if (ByteBufferUtilities.isBinaryByteBuffer(byteBuffer, byteCount)) {
            return false;
        }
        
        ByteBufferDecoder decoder = new ByteBufferDecoder(byteBuffer, byteCount);
        CharBuffer chars = decoder.getCharBuffer();
        searchCharBuffer(chars, matches);
        return true;
    }
}
