package e.util;

import java.util.*;

public class NumberDecoder {
    // FIXME: has anyone ever actually wanted to convert something to octal?
    private static final int[] OUTPUT_BASES = { 16, 10, 8, 2 };
    
    private long number;
    private int radix = 10;
    private boolean valid = false;
    
    public NumberDecoder(String string) {
        decode(string);
    }
    
    public boolean isValid() {
        return valid;
    }
    
    private void decode(String s) {
        int index = 0;
        boolean negative = false;
        
        // Handle minus sign, if present.
        if (s.startsWith("-")) {
            negative = true;
            index++;
        }
        
        // Handle radix specifier, if present.
        if (s.startsWith("0x", index) || s.startsWith("0X", index)) {
            index += 2;
            radix = 16;
        } else if (s.startsWith("#", index)) {
            ++index;
            radix = 16;
        } else if (s.startsWith("0o", index)) {
            index += 2;
            radix = 8;
        } else if (s.startsWith("0b", index)) {
            index += 2;
            radix = 2;
        } else if (s.startsWith("0", index)) {
            // A deprecated (but common) way to specify octal.
            ++index;
            radix = 8;
        }
        
        if (s.startsWith("-", index)) {
            return;
        }
        Long result;
        try {
            result = Long.valueOf(s.substring(index), radix);
            result = negative ? Long.valueOf(-result.longValue()) : result;
            number = result.longValue();
            valid = true;
        } catch (NumberFormatException ex) {
            // If number is Long.MIN_VALUE, we'll end up here. The next line
            // handles this case, and causes any genuine format error to be
            // rethrown.
            try {
                String constant = negative ? ("-" + s.substring(index)) : s.substring(index);
                result = Long.valueOf(constant, radix);
                number = result.longValue();
                valid = true;
            } catch (NumberFormatException ex2) {
                ex2 = ex2;
            }
        }
    }
    
    private static String radixToPrefix(int radix) {
        switch (radix) {
        case 2:
            return "0b";
        case 8:
            return "0o";
        case 16:
            return "0x";
        default:
            return "";
        }
    }
    
    private static String radixToName(int radix) {
        switch (radix) {
        case 2:
            return "binary";
        case 8:
            return "octal";
        case 10:
            return "decimal";
        case 16:
            return "hex";
        default:
            return "base " + radix;
        }
    }
    
    private static String toString(int radix, long number) {
        String digits = Long.toString(number, radix);
        // Make it easier to read large numbers.
        if (radix == 2) {
            // FIXME: it would be convenient if binary numbers were always a multiple of 4 digits long (i.e. no partial bytes).
            // FIXME: it would be nice to be able to see the boundary between the top and bottom 32 bits in binary.
            digits = insertCharEveryNDigits(digits, ' ', 4);
        } else  if (radix == 10) {
            // FIXME: exact powers of two would usefully be given as "2048 (2Ki)", maybe (utility increasing with magnitude, probably).
            digits = insertCharEveryNDigits(digits, ',', 3);
        } else  if (radix == 16) {
            // FIXME: it would be convenient if hex numbers were always a multiple of 2 digits long (i.e. no partial bytes).
            digits = insertCharEveryNDigits(digits, ' ', 4);
        }
        return radixToName(radix) + " " + radixToPrefix(radix) + digits;
    }
    
    private static String insertCharEveryNDigits(String s, char ch, int n) {
        StringBuilder result = new StringBuilder(s);
        for (int i = 1; i < s.length(); ++i) {
            if ((i % n) == 0) {
                result.insert(s.length() - i, ch);
            }
        }
        return result.toString();
    }
    
    private String toASCII(long number) {
        StringBuilder result = new StringBuilder("\"");
        int byteCount = ((number >> 32) != 0) ? 8 : 4;
        for (int i = 0; i < byteCount; i++) {
            char c = (char) (number & 0xff);
            if (c < ' ' || c >= 127) {
                if (c < ' ') {
                    result.insert(0, "^" + (char) (c + '@'));
                } else {
                    result.insert(0, "\\x" + Integer.toHexString(c));
                }
            } else {
                result.insert(0, c);
            }
            number >>= 8;
        }
        result.insert(0, "ASCII \"");
        return result.toString();
    }
    
    public List<String> toStrings() {
        if (valid == false) {
            return Collections.emptyList();
        }
        ArrayList<String> result = new ArrayList<String>();
        result.add(toString(radix, number) + ":");
        for (int possibleBase : OUTPUT_BASES) {
            if (possibleBase != radix) {
                result.add("    " + toString(possibleBase, number));
            }
        }
        if (radix == 16) {
            result.add("    " + toASCII(number));
        }
        return result;
    }
    
    public String toHtml() {
        if (valid == false) {
            return "";
        }
        List<String> strings = toStrings();
        StringBuilder result = new StringBuilder("<html><body>");
        for (String string : strings) {
            result.append(string);
            result.append("<br/>\n");
        }
        return result.toString().replaceAll(" ", "&nbsp;");
    }
}
