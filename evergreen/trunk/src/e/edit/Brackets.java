package e.edit;

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
public class Brackets {
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
    
    /** Returns the offset of the matching bracket, or -1. */
    public static int findMatchingBracket(CharSequence chars, int offset) {
        //Log.warn("findMatchingBracket(offset="+offset+")");
        char bracket = chars.charAt(offset);
        if (isCloseBracket(bracket)) {
            return findMatchingBracket(chars, offset, false);
        }
        if (offset > 0 && isOpenBracket(chars.charAt(offset - 1))) {
            return findMatchingBracket(chars, offset - 1, true);
        }
        return -1;
    }
    
    /** Returns the offset of the matching bracket, scanning in the given direction, or -1. */
    private static int findMatchingBracket(CharSequence chars, int offset, boolean scanForwards) {
        //Log.warn("findMatchingBracket(offset="+offset+",scanForwards="+scanForwards+")");
        char bracket = chars.charAt(offset);
        if (isBracket(bracket) == false) {
            return -1;
        }
        char partner = getPartnerForBracket(bracket);
        int nesting = 1;
        int step = scanForwards ? +1 : -1;
        int stop = scanForwards ? chars.length() : 0;
        for (offset += step; nesting != 0 && offset != stop; offset += step) {
            char ch = chars.charAt(offset);
            if (ch == bracket) {
                nesting++;
            } else if (ch == partner) {
                nesting--;
            }
        }
        if (offset != stop) {
            // We actually want to stop just before the matching bracket.
            offset += (scanForwards ? 1 : 2) * -step;
        }
        return offset;
    }
    
    private Brackets() {
        // Not for instantiation.
    }
}
