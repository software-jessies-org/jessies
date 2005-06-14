package e.util;

import java.nio.*;

/**
 * Turns a ByteBuffer into a CharSequence by pretending that each byte
 * is the equivalent Unicode character with zero for its top 16 bits.
 * If, as we do, you work a lot with ASCII, and want to go fast, this can be
 * a convenient pretense. It makes our file searching twice as fast as it
 * would otherwise be, for example.
 */
public final class AsciiCharSequence implements CharSequence {
    private ByteBuffer bytes;
    private int offset;
    private int length;
    
    public AsciiCharSequence(ByteBuffer bytes, int offset, int length) {
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }
    
    public char charAt(int index) {
        return (char) bytes.get(index + offset);
    }
    
    public int length() {
        return length;
    }
    
    public CharSequence subSequence(int start, int end) {
        return new AsciiCharSequence(bytes, offset + start, end - start);
    }
    
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length(); i++) {
            result.append(charAt(i));
        }
        return result.toString();
    }
}
