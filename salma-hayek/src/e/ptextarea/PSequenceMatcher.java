package e.ptextarea;

/**
 * PSequenceMatcher is the interface for a thing that matches strings and generates styled sequences.
 *
 * There are various implementations in here which implement various language-specific variants of comment,
 * multi-line and single-line strings, etc.
 */
public interface PSequenceMatcher {
    public RegionEnd match(String line, int fromIndex);
    
    public static class ToEndOfLineComment implements PSequenceMatcher {
        private String commentStart;
        
        ToEndOfLineComment(String commentStart) {
            this.commentStart = commentStart;
        }
        
        public RegionEnd match(String line, int fromIndex) {
            if (!line.regionMatches(fromIndex, commentStart, 0, commentStart.length())) {
                return null;
            }
            return new RegionEnd(PStyle.COMMENT, line.length());
        }
    }
    
    public static class SlashStarComment implements PSequenceMatcher {
        public RegionEnd match(String line, int fromIndex) {
            if (!line.regionMatches(fromIndex, "/*", 0, 2)) {
                return null;
            }
            // If the matching */ is on the same line, return the region with its end.
            int endIndex = line.indexOf("*/", fromIndex + 2);
            if (endIndex != -1) {
                return new RegionEnd(PStyle.COMMENT, endIndex + 2);
            }
            // Comment does not end on this line, so return a RegionEnd that can find its end.
            return new RegionEnd(PStyle.COMMENT, "*/");
        }
    }
    
    public static class CDoubleQuotes implements PSequenceMatcher {
        public RegionEnd match(String line, int fromIndex) {
            if (line.charAt(fromIndex) != '"') {
                return null;
            }
            boolean seenBackslash = false;
            for (int i = fromIndex + 1; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (ch == '\\') {
                    seenBackslash = !seenBackslash;
                    continue;
                }
                if (ch == '"' && !seenBackslash) {
                    return new RegionEnd(PStyle.STRING, i + 1);
                }
                seenBackslash = false;
            }
            return new RegionEnd(PStyle.ERROR, line.length());
        }
    }
    
    public static class PythonSingleQuotes implements PSequenceMatcher {
        public RegionEnd match(String line, int fromIndex) {
            if (line.charAt(fromIndex) != '\'') {
                return null;
            }
            boolean seenBackslash = false;
            for (int i = fromIndex + 1; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (ch == '\\') {
                    seenBackslash = !seenBackslash;
                    continue;
                }
                if (ch == '\'' && !seenBackslash) {
                    return new RegionEnd(PStyle.STRING, i + 1);
                }
                seenBackslash = false;
            }
            return new RegionEnd(PStyle.ERROR, line.length());
        }
    }
    
    public static class CSingleQuotes implements PSequenceMatcher {
        public RegionEnd match(String line, int fromIndex) {
            if (line.charAt(fromIndex) != '\'') {
                return null;
            }
            int index = fromIndex + 1;
            if (index >= line.length()) {
                return new RegionEnd(PStyle.ERROR, line.length());
            }
            char ch = line.charAt(index);
            if (ch == '\'') {
                // Empty char ('').
                return new RegionEnd(PStyle.ERROR, line.length());
            } else if (ch == '\\') {
                // There are various special ways of encoding stuff. For example, in C++ you can represent octal values with \070,
                // or hex with \x20, or unicode with \u1234 or \U12345678.
                // Right now, I can't be bothered with all the special cases, so I'm just going to say "if there's an escape,
                // we assume the following string is OK".
                for (index += 2; index < line.length(); index++) {
                    if (line.charAt(index) == '\'') {
                        return new RegionEnd(PStyle.STRING, index + 1);
                    }
                }
                return new RegionEnd(PStyle.ERROR, line.length());
                
            }
            index++;
            if (index >= line.length()) {
                return new RegionEnd(PStyle.ERROR, line.length());
            }
            if (line.charAt(index) == '\'') {
                return new RegionEnd(PStyle.STRING, index + 1);
            }
            return new RegionEnd(PStyle.ERROR, index + 1);
        }
    }
    
    public static class CppMultiLineString implements PSequenceMatcher {
        public RegionEnd match(String line, int fromIndex) {
            if (!line.regionMatches(fromIndex, "R\"", 0, 2)) {
                return null;
            }
            int delimiterStart = fromIndex + 2;
            // According to https://en.cppreference.com/w/cpp/language/string_literal :
            // In C++ multiline strings (eg R"V0G0N( .... )V0G0N"), the delimiter (in this case V0G0N) can
            // be made up of up to 16 chars of any kind except for parentheses, whitespace or backslashes.
            int delimiterEnd = delimiterStart;
            for (; delimiterEnd < line.length(); delimiterEnd++) {
                char ch = line.charAt(delimiterEnd);
                if (ch == '(') {
                    break;
                }
                if (Character.isWhitespace(ch) || ch == ')' || ch == '\\' || delimiterEnd - delimiterStart >= 16) {
                    return new RegionEnd(PStyle.ERROR, delimiterEnd);
                }
            }
            // Check if we got to the end of the line. If we did, then we're missing the open parenthesis, so this
            // multiline string is currently invalid.
            if (delimiterEnd == line.length()) {
                return new RegionEnd(PStyle.ERROR, delimiterEnd);
            }
            // If we got here, then delimiterStart is the index of the first char in the delimiter, and delimiterEnd
            // is the index of the "(" character immediately after the end of the delimiter.
            // Compose the string we expect to find, which ends the multiline string.
            String close = ")" + line.substring(delimiterStart, delimiterEnd) + "\"";
            // Can we see the close in the same line? If so, just report that.
            int closeIndex = line.indexOf(close, delimiterEnd + 1);
            if (closeIndex != -1) {
                return new RegionEnd(PStyle.STRING, closeIndex + close.length());
            }
            // It's a true multi-line string. Return the 'close' detector.
            return new RegionEnd(PStyle.STRING, close);
        }
    }
    
    /**
     * A generic multiline string matcher. This can be used for Go ("`"), Python ("'''" and "\"\"\""), or whatever.
     */
    public static class MultiLineString implements PSequenceMatcher {
        private String delimiter;
        
        MultiLineString(String delimiter) {
            this.delimiter = delimiter;
        }
        
        public RegionEnd match(String line, int fromIndex) {
            if (!line.regionMatches(fromIndex, delimiter, 0, delimiter.length())) {
                return null;
            }
            // Can we find the terminator within the same line?
            int end = line.indexOf(delimiter, fromIndex + delimiter.length());
            if (end != -1) {
                return new RegionEnd(PStyle.STRING, end + delimiter.length());
            }
            // The string doesn't end in this line, so return a matcher that will find it.
            return new RegionEnd(PStyle.STRING, delimiter);
        }
    }
    
    public static class BashHereDoc implements PSequenceMatcher {
        public RegionEnd match(String line, int fromIndex) {
            if (!line.regionMatches(fromIndex, "<<", 0, 2)) {
                return null;
            }
            // Skip over spaces until we find the delimiter.
            int delimiterStart = fromIndex + 2;
            for (; delimiterStart < line.length(); delimiterStart++) {
                if (!Character.isWhitespace(line.charAt(delimiterStart))) {
                    break;
                }
            }
            // If we got to the end of the line, this is an invalid heredoc.
            if (delimiterStart >= line.length()) {
                return new RegionEnd(PStyle.ERROR, line.length());
            }
            int delimiterEnd = delimiterStart;
            for (; delimiterEnd < line.length(); delimiterEnd++) {
                if (!isBashIdentifierChar(line.charAt(delimiterEnd))) {
                    break;
                }
            }
            return new BashHereDocRegionEnd(PStyle.STRING, line.substring(delimiterStart, delimiterEnd));
        }
        
        private boolean isBashIdentifierChar(char ch) {
            // The Java identifier stuff handles alphanumerics and underscores.
            // Bash allows other characters to be part of an identifier, though.
            return Character.isJavaIdentifierPart(ch) || ("-+".indexOf(ch) != -1);
        }
    }
    
    public static class RegionEnd {
        protected PStyle style;
        private int endIndex;
        protected String endString;
        
        public RegionEnd(PStyle style, int endIndex) {
            this.style = style;
            this.endIndex = endIndex;
        }
        
        public RegionEnd(PStyle style, String endString) {
            this.style = style;
            this.endIndex = -1;
            this.endString = endString;
        }
        
        public PStyle getStyle() {
            return style;
        }
        
        // Returns the index of the end of this region, or -1 if it ends beyond the end of the string.
        // If this method returns -1, an endString *must* be set, as this must be a multi-line thing.
        // For cases such as an unterminated string, the end index should be the end of the line, although
        // the style may be "error".
        public int getEndIndex() {
            return endIndex;
        }
        
        // Given a new line (not the one on which this region was started), find the index of the first
        // character after this region, or -1 if the region still hasn't ended.
        public int getEndIndexForLine(String line) {
            int index = line.indexOf(endString);
            return (index == -1) ? -1 : (index + endString.length());
        }
        
        public boolean equals(Object otherObj) {
            if (!(otherObj instanceof RegionEnd)) {
                return false;
            }
            RegionEnd other = (RegionEnd) otherObj;
            return areEqual(style, other.style) && endIndex == other.endIndex && areEqual(endString, other.endString);
        }
        
        public int hashCode() {
            int result = 19293 * endIndex;
            if (style != null) {
                result ^= style.hashCode();
            }
            if (endString != null) {
                result ^= endString.hashCode();
            }
            return result;
        }
        
        protected static boolean areEqual(Object one, Object two) {
            return (one == null || two == null) ? (one == two) : one.equals(two);
        }
    }
    
    // Bash 'HereDoc' things are special in that they absolutely require the terminator to be on its own on a line,
    // otherwise they don't match.
    public static class BashHereDocRegionEnd extends RegionEnd {
        public BashHereDocRegionEnd(PStyle style, String endString) {
            super(style, endString);
        }
        
        @Override public int getEndIndexForLine(String line) {
            return line.equals(endString) ? line.length() : -1;
        }
    }
}
