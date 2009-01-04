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
    
    public static boolean isAllowedCharInBracket(char character, char bracket) {
        switch (bracket) {
        case '<':
        case '>':
            // Strictly speaking, this isn't a valid optimization for C++
            // but it's unlikely we'll ever see a failure case and
            // Phil thinks it "should" speed things up.
            return ("{}[]()".indexOf(character) == -1);
            
        default:
            return true;
        }
    }
    
    public static boolean isNestableBracketInBracket(char innerBracket, char outerBracket) {
        if ("[]{}()".indexOf(outerBracket) != -1) {
            return ("[]{}()".indexOf(innerBracket) != -1);
        } else if ("<>".indexOf(outerBracket) != -1) {
            return ("<>".indexOf(innerBracket) != -1);
        } else {
            throw new IllegalArgumentException("Character " + outerBracket + " is not a bracket.");
        }
    }
    
    /**
     * Returns the offset of the matching bracket, or -1 if there's no match.
     * We look for a match if 'offset' is after an opening bracket, or before a
     * closing bracket. An exception is thrown if we're not next to a bracket.
     */
    public static int findMatchingBracketInSameStyle(PTextArea textArea, int offset) {
        if (afterOpenBracket(textArea.getTextBuffer(), offset)) {
            Iterator<PLineSegment> segments = textArea.getLogicalSegmentIterator(offset - 1);
            segments = new PSameStyleSegmentIterator(segments);
            return findMatchingBracket(new PSegmentCharIterator(segments, offset - 1, true), true);
            
        } else if (beforeCloseBracket(textArea.getTextBuffer(), offset)) {
            Iterator<PLineSegment> segments = new PReverseSegmentIterator(textArea, offset);
            segments = new PSameStyleSegmentIterator(segments);
            return findMatchingBracket(new PSegmentCharIterator(segments, offset, false), false);
            
        } else {
            throw new IllegalArgumentException("No bracket at offset " + offset);
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
    private static int findMatchingBracket(PCharIterator chars, boolean isForward) {
        char bracket = chars.next();
        if (isBracket(bracket) == false) {
            return -1;
        }
        Stack<Character> bracketStack = new Stack<Character>();
        bracketStack.push(bracket);
        while (chars.hasNext()) {
            char ch = chars.next();
            if (isAllowedCharInBracket(ch, bracket)) {
                if (isNestableBracketInBracket(ch, bracket)) {
                    if (isOpenBracket(ch) == isForward) {
                        bracketStack.push(ch);
                    } else {
                        char requiredMatch = getPartnerForBracket(bracketStack.pop());
                        if (requiredMatch != ch) {
                            return -1;  // The nesting of brackets is wrong.
                        } else if (bracketStack.empty()) {
                            return chars.getOffsetOfLastChar();
                        }
                    }
                }
            } else {
                return -1;  // Illegal characters in the way: we're not going to find a match here.
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
        return (offset > 0 && offset - 1 < chars.length() && isOpenBracket(chars.charAt(offset - 1)));
    }
    
    private PBracketUtilities() {
        // Not for instantiation.
    }
}
