package e.ptextarea;

public class PWordUtilities {
    private static final String WHITESPACE = " \t";
    
    public static final String DEFAULT_STOP_CHARS = " \t\n!\"#%&'()*+,-./:;<=>?@`[\\]^{|}~";
    
    /** Returns the start of the word at 'offset'. */
    public static final int getWordStart(CharSequence text, int offset, String stopChars) {
        return scanBackwards(text, offset, stopChars, false);
    }
    
    /** Returns the end of the word at 'offset'. */
    public static final int getWordEnd(CharSequence text, int offset, String stopChars) {
        return scanForwards(text, offset, stopChars, false);
    }
    
    /** Returns the start of the whitespace at 'offset'. */
    public static final int getWhitespaceStart(CharSequence text, int offset) {
        return scanBackwards(text, offset, WHITESPACE, true);
    }
    
    /** Returns the start of the non-word at 'offset'. */
    public static final int getNonWordStart(CharSequence text, int offset, String stopChars) {
        return scanBackwards(text, offset, stopChars, true);
    }
    
    /** Returns the end of the non-word at 'offset'. */
    public static final int getNonWordEnd(CharSequence text, int offset, String stopChars) {
        return scanForwards(text, offset, stopChars, true);
    }
    
    private static final boolean charIsFoundIn(char c, String s) {
        return s.indexOf(c) != -1;
    }
    
    /** Tests whether the given offset is in a word in 'text'. */
    public static final boolean isInWord(CharSequence text, int offset, String stopChars) {
        return (isIn(text, offset, stopChars) == false);
    }
    
    /** Tests whether the given offset is in whitespace in 'text'. */
    public static final boolean isInWhitespace(CharSequence text, int offset) {
        return isIn(text, offset, WHITESPACE);
    }
    
    private static final boolean isIn(CharSequence text, int offset, String characters) {
        return charIsFoundIn(text.charAt(offset), characters);
    }
    
    private static final int scanBackwards(CharSequence text, int offset, String set, boolean shouldBeInSet) {
        while (offset > 0 && charIsFoundIn(text.charAt(offset - 1), set) == shouldBeInSet) {
            offset--;
        }
        return offset;
    }
    
    private static final int scanForwards(CharSequence text, int offset, String set, boolean shouldBeInSet) {
        int lastOffset = text.length();
        while (offset < lastOffset && charIsFoundIn(text.charAt(offset), set) == shouldBeInSet) {
            offset++;
        }
        return offset;
    }
    
    private PWordUtilities() {
        // FIXME: this should be something that each PTextArea has an instance of.
    }
}
