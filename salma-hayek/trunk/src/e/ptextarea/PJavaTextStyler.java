package e.ptextarea;

import java.util.*;

/**
 * A PJavaTextStyler knows how to apply syntax highlighting for Java code.
 * 
 * @author Phil Norman
 */

public class PJavaTextStyler extends PCLikeTextStyler {
    
    private static final String[] KEYWORDS = new String[] {
        // JLS3, section 3.9: "Keywords"
        "abstract",
        "assert",
        "boolean",
        "break",
        "byte",
        "case",
        "catch",
        "char",
        "class",
        "const",
        "continue",
        "default",
        "do",
        "double",
        "else",
        "enum",
        "extends",
        "final",
        "finally",
        "float",
        "for",
        "if",
        "goto",
        "implements",
        "import",
        "instanceof",
        "int",
        "interface",
        "long",
        "native",
        "new",
        "package",
        "private",
        "protected",
        "public",
        "return",
        "short",
        "static",
        "strictfp",
        "super",
        "switch",
        "synchronized",
        "this",
        "throw",
        "throws",
        "transient",
        "try",
        "void",
        "volatile",
        "while",
        
        // JLS3, section 3.10.3: "Boolean Literals"
        "true", "false",
        
        // JLS3, section 3.10.7: "The Null Literal"
        "null",
    };
    
    public PJavaTextStyler(PTextArea textArea) {
        super(textArea);
        addKeywords(KEYWORDS);
    }
    
    /**
     * Overrides the default string segment addition, performing validation on the
     * string format and introducing error segment types where appropriate.
     */
    public void addStringSegment(ArrayList segmentList, String string) {
        if (string.startsWith("\'")) {
            addSegments(segmentList, string, 1);
        } else {
            addSegments(segmentList, string, Integer.MAX_VALUE);
        }
    }
    
    private void addSegments(ArrayList segmentList, String string, int maxChars) {
        int segmentStart = 0;
        boolean segmentIsValid = true;
        int charCount = 0;
        // The increment step is done inside the loop.
        for (int i = 1; i < string.length() - 1; ) {
            boolean isError = false;
            int increment;
            if (charCount >= maxChars) {
                isError = true;
                increment = 1;
            } else {
                // Note - we may make the assumption here that there is at least one readable character
                // after the escaping '\', since otherwise the line parser would have understood it as
                // escaping our terminating ' or ".
                if (string.charAt(i) == '\\') {
                    if (isValidOctalDigit(string.charAt(i + 1))) {
                        int maxDigits = (string.charAt(i + 1) <= '3') ? 3 : 2;
                        for (increment = 1; increment <= maxDigits; increment++) {
                            if (isValidOctalDigit(string.charAt(i + increment)) == false) {
                                break;
                            }
                        }
                    } else if (isBasicEscapeCharacter(string.charAt(i + 1))) {
                        increment = 2;
                    } else if (string.charAt(i + 1) == 'u') {
                        // From section 3.3 (Unicode Escapes) of the JLS.
                        // A unicode escape always consists of a backslash, then a 'u', followed by exactly 4 hexadecimal digits.
                        increment = 6;
                        for (int j = 0; j < 4; j++) {
                            if (isValidHexDigit(string.charAt(i + 2 + j)) == false) {
                                increment = 2 + j;
                                isError = true;
                                break;
                            }
                        }
                        if (isError == false) {
                            // Cope with certain unicode escapes which are considered erroneous.
                            isError = isInvalidUnicodeEscape(string.substring(i, i + increment), string.charAt(0));
                        }
                    } else {
                        // Invalid escape sequence - we presume the user meant to use a two-character
                        // escape sequence; it'll probably be the best way to indicate what the error is due to
                        // anyway.
                        isError = true;
                        increment = 2;
                    }
                } else {
                    increment = 1;
                }
            }
            if (isError) {
                if (segmentIsValid) {
                    segmentList.add(new PTextSegment(PStyle.STRING, string.substring(segmentStart, i)));
                    segmentStart = i;
                    segmentIsValid = false;
                }
            } else {
                if (segmentIsValid == false) {
                    segmentList.add(new PTextSegment(PStyle.ERROR, string.substring(segmentStart, i)));
                    segmentStart = i;
                    segmentIsValid = true;
                }
            }
            charCount++;
            i += increment;
        }
        if (segmentIsValid == false) {
            segmentList.add(new PTextSegment(PStyle.ERROR, string.substring(segmentStart, string.length() - 1)));
            segmentStart = string.length() - 1;
        }
        segmentList.add(new PTextSegment(PStyle.STRING, string.substring(segmentStart)));
    }
    
    private boolean isInvalidUnicodeEscape(String escapeHex, char quoteType) {
        // From section 3.10.4 (Character Literals) of the JLS:
        // Because unicode escapes are processed very early in the compilation, it is
        // not valid to use them to represent newlines or carriage returns, since they would
        // be transformed into their line-terminating character equivalents before the string
        // is parsed.
        String matchingQuoteUnicode = (quoteType == '"') ? "[\\\\]u0022" : "[\\\\]u0027";
        return escapeHex.matches("[\\\\]u000[AaDd]") || escapeHex.matches(matchingQuoteUnicode);
    }
    
    private boolean isBasicEscapeCharacter(char ch) {
        // From section 3.10.6 (Escape Sequences for Character and String Literals) of the JLS.
        switch (ch) {
        case 'b':  // Backspace
        case 't':  // Tab
        case 'n':  // Linefeed
        case 'f':  // Form feed
        case 'r':  // Carriage return
        case '"':  // Double-quote
        case '\'':  // Single-quote
        case '\\':  // Backslash
            return true;
            
        default:
            return false;
        }
    }
    
    private boolean isValidOctalDigit(char ch) {
        return (ch >= '0' && ch <= '7');
    }
    
    private boolean isValidHexDigit(char ch) {
        return ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F'));
    }

    public boolean supportShellComments() {
        return false;
    }
}
