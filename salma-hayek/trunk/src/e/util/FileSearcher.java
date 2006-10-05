package e.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
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
    
    private boolean isBinaryByteBuffer(ByteBuffer byteBuffer, final int byteCount) {
        // Check we haven't accidentally come across a binary file.
        final int end = Math.min(byteCount, 16);
        for (int i = 0; i < end; i++) {
            if (byteBuffer.get(i) == 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Search for occurrences of the input pattern in the given file.
     * Returns false if unable to search; true otherwise.
     */
    public boolean searchFile(File file, Collection<String> matches) throws IOException {
        int byteCount = (int) file.length();
        
        DataInputStream dataInputStream = null;
        FileChannel fileChannel = null;
        ByteBuffer byteBuffer = null;
        
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            
            // FIXME: we should measure where the best cut-off point is.
            if (byteCount <= 4096) {
                // Read the whole file in.
                dataInputStream = new DataInputStream(fileInputStream);
                byteBuffer = ByteBuffer.wrap(new byte[byteCount]);
                dataInputStream.readFully(byteBuffer.array());
            } else {
                // Map the file into memory.
                fileChannel = fileInputStream.getChannel();
                byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, byteCount);
            }
            
            if (isBinaryByteBuffer(byteBuffer, byteCount)) {
                return false;
            }
            
            // FIXME: we should merge this code and the similar (but better) code in PTextBuffer...
            CharBuffer chars = null;
            try {
                CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
                chars = decoder.decode(byteBuffer);
            } catch (Exception unused) {
                byteBuffer.rewind();
                try {
                    CharsetDecoder decoder = Charset.forName("ISO-8859-1").newDecoder();
                    chars = decoder.decode(byteBuffer);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            //CharSequence chars = new AsciiCharSequence(byteBuffer, 0, byteCount);
            
            searchCharBuffer(chars, matches);
        } finally {
            if (fileChannel != null) {
                fileChannel.close();
            }
            if (dataInputStream != null) {
                dataInputStream.close();
            }
        }
        return true;
    }
}
