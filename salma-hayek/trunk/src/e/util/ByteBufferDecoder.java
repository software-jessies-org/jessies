package e.util;

import java.nio.*;
import java.nio.charset.*;

/**
 * Translates a ByteBuffer into a CharBuffer or char[], trying to guess the character encoding from the content.
 */
public class ByteBufferDecoder {
    private ByteBuffer byteBuffer;
    private long byteCount;
    
    private String encoding;
    private CharBuffer charBuffer;
    private char[] charArray;
    
    public ByteBufferDecoder(ByteBuffer byteBuffer, long byteCount) {
        this.byteBuffer = byteBuffer;
        this.byteCount = byteCount;
    }
    
    /**
     * Returns a CharBuffer (which implements CharSequence) containing the characters found in the ByteBuffer.
     */
    public CharBuffer getCharBuffer() {
        if (charBuffer == null) {
            charBuffer = decodeByteBuffer();
        }
        return charBuffer;
    }
    
    /**
     * Returns a char[] containing the characters found in the ByteBuffer.
     */
    public char[] getCharArray() {
        if (charArray == null) {
            if (charBuffer == null) {
                getCharBuffer();
            }
            charArray = extractCharArray(charBuffer);
        }
        return charArray;
    }
    
    /**
     * Returns the character encoding assumed when decoding the ByteBuffer.
     */
    public String getEncodingName() {
        if (encoding == null) {
            getCharBuffer();
        }
        return encoding;
    }
    
    private CharBuffer decodeByteBuffer() {
        // Assume UTF-8, but check for a UTF-16 BOM.
        String charsetName = "UTF-8";
        if (byteCount > 1) {
            int possibleBom = byteBuffer.getShort(0) & 0xffff;
            if (possibleBom == 0xfeff) {
                charsetName = "UTF-16BE";
            } else if (possibleBom == 0xfffe) {
                charsetName = "UTF-16LE";
            }
        }
        
        try {
            return attemptDecoding(byteBuffer, charsetName);
        } catch (Exception unused) {
            // Try again with the most popular parochial format (in my part of
            // the world). The various parochial formats will almost certainly
            // decode without error (in the sense that they'll accept pretty
            // much any byte sequence), but the result may be gibberish. I
            // think this is the best we can do, programmatically, though it's
            // quite tempting to just let attemptDecoding throw an exception
            // and encourage people to recode non-UTF files.
            byteBuffer.rewind();
            try {
                return attemptDecoding(byteBuffer, "ISO-8859-1");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    private CharBuffer attemptDecoding(ByteBuffer byteBuffer, String charsetName) throws CharacterCodingException {
        CharsetDecoder decoder = makeReportingCharsetDecoder(charsetName);
        CharBuffer charBuffer = decoder.decode(byteBuffer);
        encoding = charsetName;
        return charBuffer;
    }
    
    private static CharsetDecoder makeReportingCharsetDecoder(String charsetName) {
        // CharsetDecoder is reporting by default.
        return Charset.forName(charsetName).newDecoder();
    }
    
    private static char[] extractCharArray(CharBuffer charBuffer) {
        // See if we need to copy the characters into a new char[].
        if (charBuffer.hasArray() == false || charBuffer.isReadOnly()) {
            char[] chars = new char[charBuffer.length()];
            for (int i = 0; i < chars.length; ++i) {
                chars[i] = charBuffer.charAt(i);
            }
            return chars;
        }
        
        // We may be able to use the buffer's char[] directly.
        char[] chars = charBuffer.array();
        
        // The 1.5 UTF-8 decoder leaves us with a directly usable char[],
        // but the UTF-16 decoders don't because they swallow the
        // BOM, so in those cases we need to re-copy the relevant portion.
        // We could fix that by skipping the BOM when we read it in,
        // but I'd rather have this safety net. UTF-8 is the expected
        // format anyway, and this won't harm UTF-8 performance.
        if (chars.length != charBuffer.length()) {
            char[] newChars = new char[charBuffer.length()];
            System.arraycopy(chars, charBuffer.arrayOffset(), newChars, 0, newChars.length);
            chars = newChars;
        }
        return chars;
    }
}
