package e.util;

import java.util.*;

public class NumberDecoder {
    private long number;
    private int radix = 10;
    private boolean valid = false;
    private String problem;
    
    public NumberDecoder(String string) {
        decode(string);
    }
    
    public boolean isValid() {
        return valid;
    }
    
    private void decode(String s) {
        int index = 0;
        boolean negative = false;
        
        // Handle minus sign, if present
        if (s.startsWith("-")) {
            negative = true;
            index++;
        }
        
        // Handle radix specifier, if present
        if (s.startsWith("0x", index) || s.startsWith("0X", index)) {
            index += 2;
            radix = 16;
        } else if (s.startsWith("#", index)) {
            index++;
            radix = 16;
        } else if (s.startsWith("0", index)) {
            index++;
            radix = 8;
        }
        
        if (s.startsWith("-", index)) {
            return;
        }
        Long result;
        try {
            result = Long.valueOf(s.substring(index), radix);
            result = negative ? new Long((long)-result.longValue()) : result;
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
            case 8: return "0";
            case 16: return "0x";
        default:
            return "";
        }
    }
    
    private static String radixToName(int radix) {
        switch (radix) {
            case 2: return "binary";
            case 8: return "octal";
            case 10: return "decimal";
            case 16: return "hex";
        default:
            return "base " + radix;
        }
    }
    
    private int[] bases = { 16, 10, 8, 2 };
    
    private static String toString(int radix, long number) {
        return radixToName(radix) + " " + radixToPrefix(radix) + Long.toString(number, radix);
    }
    
    private String toASCII(long number) {
        StringBuffer result = new StringBuffer("\"");
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
    
    public List toStrings() {
        if (valid == false) {
            return Collections.EMPTY_LIST;
        }
        ArrayList result = new ArrayList();
        result.add(toString(radix, number) + ":");
        for (int i = 0; i < bases.length; i++) {
            if (bases[i] != radix) {
                result.add("    " + toString(bases[i], number));
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
        List strings = toStrings();
        StringBuffer result = new StringBuffer("<html>");
        for (int i = 0; i < strings.size(); ++i) {
            String string = (String) strings.get(i);
            result.append(string);
            result.append("<br>\n");
        }
        return result.toString().replaceAll(" ", "&nbsp;");
    }
}
