package e.edit;

public class NumberResearcher implements WorkspaceResearcher {
    /**
     * Look for something in a JTextComponent. Returns an HTML string
     * containing information about what it found. Should return
     * the empty string (not null) if it has nothing to say.
     */
    public String research(javax.swing.text.JTextComponent text, String string) {
        try {
            DecodedNumber number = new DecodedNumber(string);
            //Log.warn("result='" + number.toString() + "'");
            return number.toString();
        } catch (Exception ex) {
            return "";
        }
    }
    
    /** Returns true, because numbers are everywhere. */
    public boolean isSuitable(ETextWindow textWindow) {
        return true;
    }

    public class DecodedNumber {
        private long number;
        private int radix = 10;
        private boolean valid = false;
        private String problem;
        
        public DecodedNumber(String string) {
            decode(string);
        }
        
        public void decode(String s) {
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
                problem = "Negative sign in wrong position";
                return;
            }
            Long result;
            try {
                result = Long.valueOf(s.substring(index), radix);
                result = negative ? new Long((long)-result.longValue()) : result;
                number = result.longValue();
                valid = true;
            } catch (NumberFormatException e) {
                // If number is Long.MIN_VALUE, we'll end up here. The next line
                // handles this case, and causes any genuine format error to be
                // rethrown.
                String constant = negative ? ("-" + s.substring(index)) : s.substring(index);
                result = Long.valueOf(constant, radix);
                number = result.longValue();
                valid = true;
            }
        }
        
        public String radixToPrefix(int radix) {
            switch (radix) {
                case 8: return "0";
                case 16: return "0x";
                default:
                    return "";
            }
        }
        
        public String radixToName(int radix) {
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
        
        public String toString(int radix, long number) {
            return radixToName(radix) + " " + radixToPrefix(radix) + Long.toString(number, radix);
        }
        
        public String toASCII(long number) {
            StringBuffer result = new StringBuffer("\"</tt>");
            int byteCount = ((number >> 32) != 0) ? 8 : 4;
            for (int i = 0; i < byteCount; i++) {
                char c = (char) (number & 0xff);
                if (c < ' ' || c >= 127) {
                    char replacementChar = (c < ' ') ? (char)(c + '@') : '.';
                    result.insert(0, "<font color=\"red\">" + replacementChar + "</font>");
                } else {
                    result.insert(0, c);
                }
                number >>= 8;
            }
            result.insert(0, "ASCII <tt>\"");
            return result.toString();
        }
        
        public String toString() {
            if (valid) {
                StringBuffer result = new StringBuffer();
                result.append(toString(radix, number));
                result.append(":<br>");
                for (int i = 0; i < bases.length; i++) {
                    if (bases[i] != radix) {
                        result.append("&nbsp;&nbsp;&nbsp;&nbsp;" + toString(bases[i], number) + "<br>");
                    }
                }
                if (radix == 16) {
                    result.append("&nbsp;&nbsp;&nbsp;&nbsp;" + toASCII(number) + "<br>");
                }
                return result.toString();
            } else {
                return problem;
            }
        }
    }
}
