package e.util;

/**
 * Wraps a char[] as a CharSequence for convenience.
 */
public class CharArrayCharSequence implements CharSequence {
    private char[] chars;
    private int offset;
    private int length;
    
    /**
     * Constructs a sequence from all the chars in the given array. The array
     * is not copied, so if it's mutated, so is this sequence.
     */
    public CharArrayCharSequence(char[] chars) {
        this(chars, 0, chars.length);
    }
    
    /**
     * Constructs a sequence containing 'length' chars starting from
     * offset 'offset' in the array 'chars'. The array is not copied, so
     * if it's mutated, so is this sequence.
     */
    public CharArrayCharSequence(char[] chars, int offset, int length) {
        this.chars = chars;
        this.offset = offset;
        this.length = length;
    }
    
    /**
     * Returns the char at the given index.
     */
    public char charAt(int index) {
        return chars[offset + index];
    }
    
    /**
     * Returns the length of this sequence.
     */
    public int length() {
        return length;
    }
    
    /**
     * Returns a new sequence in the given interval. The char[] is not copied,
     * so if the array that created this sequence is mutated, so are any of
     * its subsequences.
     */
    public CharSequence subSequence(int start, int end) {
        return new CharArrayCharSequence(chars, start + offset, end + offset);
    }
    
    /**
     * Returns a string containing all the characters in this sequence. The
     * characters are copied; further mutations of the array backing this
     * sequence will have no effect on the string.
     */
    public String toString() {
        return new String(chars, offset, length);
    }
}
