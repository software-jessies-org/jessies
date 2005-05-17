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
    
    /**
     * Returns the offset of the matching bracket, or -1.
     * We look for a match if 'offset' is after an opening bracket, or before a
     * closing bracket.
     */
    public static int findMatchingBracket(CharSequence chars, int offset) {
        if (offset < chars.length() && isCloseBracket(chars.charAt(offset))) {
            return findMatchingBracket(chars, offset, false);
        }
        if (offset > 0 && isOpenBracket(chars.charAt(offset - 1))) {
            return findMatchingBracket(chars, offset - 1, true);
        }
        return -1;
    }
    
    /**
     * Returns the offset of the matching bracket, scanning in the given
     * direction, or -1.
     */
    private static int findMatchingBracket(CharSequence chars, final int startOffset, boolean scanForwards) {
        char bracket = chars.charAt(startOffset);
        if (isBracket(bracket) == false) {
            return -1;
        }
        char partner = getPartnerForBracket(bracket);
        int nesting = 0;
        int step = scanForwards ? +1 : -1;
        int onePastTheEnd = scanForwards ? chars.length() : -1;
        for (int offset = startOffset; offset != onePastTheEnd; offset += step) {
            char ch = chars.charAt(offset);
            if (ch == bracket) {
                ++nesting;
            } else if (ch == partner) {
                --nesting;
                if (nesting == 0) {
                    return offset;
                }
            }
        }
        return -1;
    }
    
    public static String reflectBrackets(String originalBrackets) {
        StringBuffer reflectedBrackets = new StringBuffer();
        for (int i = 0; i != originalBrackets.length(); ++i) {
            char originalBracket = originalBrackets.charAt(i);
            char reflectedBracket = PBracketUtilities.getPartnerForBracket(originalBracket);
            reflectedBrackets.append(reflectedBracket);
        }
        return reflectedBrackets.reverse().toString();
    }
    
    public static boolean beforeCloseBracket(CharSequence chars, int offset) {
        return (offset < chars.length() && isCloseBracket(chars.charAt(offset)));
    }
    
    public static boolean afterOpenBracket(CharSequence chars, int offset) {
        return (offset > 0 && isOpenBracket(chars.charAt(offset - 1)));
    }
    
    private PBracketUtilities() {
        // Not for instantiation.
    }
}
