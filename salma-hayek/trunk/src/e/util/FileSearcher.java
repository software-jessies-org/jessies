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
    private int searchCharBuffer(String fileName, CharSequence charSequence, Collection matches) {
        // Early exit on non-matching files.
        Matcher firstMatch = pattern.matcher(charSequence);
        if (firstMatch.find() == false) {
            return 0;
        }
        
        Matcher patternMatcher = null;
        int matchCount = 0;
        int start = 0;
        for (int lineNumber = 1; start < charSequence.length(); lineNumber++) {
            int end = findEndOfLine(charSequence, start);
            CharSequence currentLine = charSequence.subSequence(start, end);
            if (patternMatcher == null) {
                patternMatcher = pattern.matcher(currentLine);
            } else {
                patternMatcher.reset(currentLine);
            }
            if (patternMatcher.find()) {
                matches.add(":" + lineNumber + ":" + currentLine);
                ++matchCount;
            }
            
            start = end + 1;
        }
        return matchCount;
    }
    
    /**
     * Search for occurrences of the input pattern in the given file.
     * Returns the number of matches.
     */
    public int searchFile(String path, String fileName, Collection matches) throws IOException {
        File file = FileUtilities.fileFromParentAndString(path, fileName);
        FileInputStream fileInputStream = new FileInputStream(file);
        DataInputStream dataInputStream = new DataInputStream(fileInputStream);
        //FileChannel fileChannel = fileInputStream.getChannel();
        try {
            int byteCount = (int) file.length();
            ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[byteCount]);
            dataInputStream.readFully(byteBuffer.array());
            
            // Map the file into memory.
            //int byteCount = (int) fileChannel.size();
            //MappedByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, byteCount);
            
            // Check we haven't accidentally come across a binary file.
            final int end = Math.min(byteCount, 16);
            for (int i = 0; i < end; i++) {
                if (byteBuffer.get(i) == 0) {
                    //System.err.println("--Rejecting binary " + fileName);
                    return 0;
                }
            }
            
            CharSequence charSequence = new AsciiCharSequence(byteBuffer, 0, byteCount);
            return searchCharBuffer(fileName, charSequence, matches);
        } finally {
            //fileChannel.close();
            dataInputStream.close();
        }
    }
}
