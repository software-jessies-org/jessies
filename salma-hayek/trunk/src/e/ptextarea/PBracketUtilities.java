package e.ptextarea;

import java.util.*;

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
    
    public static int findMatchingBracketInSameStyle(PTextArea textArea, int offset)
    {
        StopWatch watch = new StopWatch();
        try {
            CharSequence chars = textArea.getTextBuffer();
            PCharIterator iterator;
            if (afterOpenBracket(chars, offset)) {
                Iterator<PLineSegment> segments = new PSameStyleSegmentIterator(textArea.getLogicalSegmentIterator(offset - 1));
                iterator = new PSegmentCharIterator(segments, offset - 1, true);
            } else if (beforeCloseBracket(chars, offset)) {
                iterator = new PCharSequenceIterator(PSameStyleCharSequence.forOffset(textArea, offset), offset, false);
            } else {
                throw new IllegalArgumentException("No bracket at offset " + offset);
            }
            return findMatchingBracket(iterator);
        } finally {
            watch.print("Matching brackets");
        }
    }
    
    /**
     * Returns true when the given offset is either just to the right of an open
     * bracket, or just to the left of a close bracket.
     */
    public static boolean isNextToBracket(CharSequence chars, int offset) {
        return (beforeCloseBracket(chars, offset) || afterOpenBracket(chars, offset));
    }
    
    /**
     * Returns the offset of the matching bracket, scanning in the given
     * direction, or -1.
     */
    private static int findMatchingBracket(PCharIterator chars) {
        char bracket = chars.next();
        if (isBracket(bracket) == false) {
            return -1;
        }
        char partner = getPartnerForBracket(bracket);
        int nesting = 1;
        while (chars.hasNext()) {
            char ch = chars.next();
            if (ch == bracket) {
                ++nesting;
            } else if (ch == partner) {
                --nesting;
                if (nesting == 0) {
                    return chars.getOffsetOfLastChar();
                }
            }
        }
        return -1;
    }
    
    public static String reflectBrackets(String originalBrackets) {
        StringBuilder reflectedBrackets = new StringBuilder();
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
