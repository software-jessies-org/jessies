package e.ptextarea;

/**
 * Utilities related to bracket-matching.
 * 
 * Some handy examples to try out bracket matching on:
 * 
 * if (hello > (int) )
 * if (hello > (int))
 * if ((int) x > 2)
 * if ( (int) x > 2)
 */
public class PBracketUtilities {
    private static final String BRACKETS = "(<[{}]>)";
    
    private static final String PARTNERS = ")>]}{[<(";
    
    public static boolean isBracket(char ch) {
        return BRACKETS.indexOf(ch) != -1;
    }
    
    public static boolean isOpenBracket(char ch) {
        return BRACKETS.indexOf(ch) < PARTNERS.indexOf(ch);
    }
    
    public static boolean isCloseBracket(char ch) {
        return isBracket(ch) && isOpenBracket(ch) == false;
    }
    
    public static char getPartnerForBracket(char bracket) {
        return PARTNERS.charAt(BRACKETS.indexOf(bracket));
    }
    
    /** Returns the offset *inside* the matching bracket, or -1. */
    public static int findMatchingBracket(CharSequence chars, int offset) {
        if (offset >= chars.length()) {
            // In most editors, you can position the caret after the last
            // character in the buffer, so it probably makes sense to cope.
            return -1;
        }
        char bracket = chars.charAt(offset);
        if (isCloseBracket(bracket)) {
            return findMatchingBracket(chars, offset, false);
        }
        if (offset > 0 && isOpenBracket(chars.charAt(offset - 1))) {
            return findMatchingBracket(chars, offset - 1, true);
        }
        return -1;
    }
    
    /** Returns the offset *inside* the matching bracket, scanning in the given direction, or -1. */
    private static int findMatchingBracket(CharSequence chars, final int startOffset, boolean scanForwards) {
        //Log.warn("findMatchingBracket(offset="+startOffset+",scanForwards="+scanForwards+")");
        char bracket = chars.charAt(startOffset);
        if (isBracket(bracket) == false) {
            return -1;
        }
        char partner = getPartnerForBracket(bracket);
        int nesting = 0;
        int step = scanForwards ? +1 : -1;
        // "stop" is one past the last, STL-style.
        int stop = scanForwards ? chars.length() : -1;
        for (int offset = startOffset; offset != stop; offset += step) {
            char ch = chars.charAt(offset);
            if (ch == bracket) {
                nesting++;
            } else if (ch == partner) {
                nesting--;
                if (nesting == 0) {
                    // We want to stop inside the matching bracket.
                    // If we're going forwards, that's the offset of the matching bracket.
                    // If we're going backwards, that's the offset after the matching bracket.
                    if (scanForwards == false) {
                        offset++;
                    }
                    return offset;
                }
            }
        }
        return -1;
    }
    
    private PBracketUtilities() {
        // Not for instantiation.
    }
}
