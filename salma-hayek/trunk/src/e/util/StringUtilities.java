package e.util;

import java.io.*;
import java.nio.*;
import java.util.*;

public class StringUtilities {
    /** Reads all the lines from the named file into a string array. Throws a RuntimeException on failure. */
    public static String[] readLinesFromFile(String filename) {
        String contents = readFile(filename);
        // The empty file clearly contains no lines but Java's split (unlike Ruby's) would give us a singleton array containing the empty string.
        if (contents.length() == 0) {
            return new String[0];
        }
        return contents.split("\n");
    }
    
    /** Reads the entire contents of the named file into a String. Throws a RuntimeException on failure. */
    public static String readFile(String filename) {
        return readFile(FileUtilities.fileFromString(filename));
    }
    
    /** Reads the entire contents of the given File into a String. Throws a RuntimeException on failure. */
    public static String readFile(File file) {
        try {
            int byteCount = (int) file.length();
            ByteBuffer byteBuffer = ByteBufferUtilities.readFile(file);
            if (ByteBufferUtilities.isBinaryByteBuffer(byteBuffer, byteCount)) {
                throw new RuntimeException("binary file");
            }
            ByteBufferDecoder decoder = new ByteBufferDecoder(byteBuffer, byteCount);
            // FIXME: we could save some copying if we could return a CharSequence.
            return new String(decoder.getCharArray());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /** Writes the given String to the given File. Returns the text of the exception message on failure, null on success. */
    public static String writeFile(File file, CharSequence content) {
        return writeFile(file, content, false);
    }
    
    /** Appends the given String to the given File. Returns the text of the exception message on failure, null on success. */
    public static String appendToFile(File file, CharSequence content) {
        return writeFile(file, content, true);
    }
    
    private static String writeFile(File file, CharSequence content, boolean append) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, append), "UTF-8"));
            out.print(content);
            return null;
        } catch (IOException ex) {
            return ex.getMessage();
        } finally {
            FileUtilities.close(out);
        }
    }
    
    /** Used by the appendUnicodeHex to pad Unicode escapes to four digits. */
    private static final char[] ZEROES = { '0', '0', '0' };
    
    /** Appends the Java Unicode escape sequence for 'ch' to 'result' ("\u23cf", say). */
    public static void appendUnicodeEscape(StringBuilder result, char ch) {
        result.append("\\u");
        appendUnicodeHex(result, ch);
    }
    
    /** Appends the 4 hex digits for the char 'ch' to 'result' ("23cf", say). */
    public static void appendUnicodeHex(StringBuilder result, char ch) {
        String digits = Integer.toString((int) ch, 16);
        result.append(ZEROES, 0, 4 - digits.length());
        result.append(digits);
    }
    
    /** Turns a string into a printable Java string literal (minus the quotes). So a tab is converted to "\t", et cetera. */
    public static String escapeForJava(CharSequence s) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                result.append("\\\\");
            } else if (c == '\n') {
                result.append("\\n");
            } else if (c == '\r') {
                result.append("\\r");
            } else if (c == '\t') {
                result.append("\\t");
            } else if (c < ' ' || c > '~') {
                appendUnicodeEscape(result, c);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    /** Turns a printable Java string literal (minus the quotes) into a string. So "\t" is converted to a tab, et cetera. */
    public static String unescapeJava(String s) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i < s.length() - 1) {
                char next = s.charAt(++i);
                if (next == 'n') {
                    result.append('\n');
                } else if (next == 'r') {
                    result.append('\r');
                } else if (next == 't') {
                    result.append('\t');
                } else if (next == 'u') {
                    char actualChar = (char) Integer.parseInt(s.substring(++i, i + 4), 16);
                    i += 3;
                    result.append(actualChar);
                } else {
                    result.append(next);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    /** Converts a string into a regular expression matching exactly that string. */
    public static String regularExpressionFromLiteral(CharSequence literal) {
        StringBuilder result = new StringBuilder();
        final String REGEXP_META_CHARACTERS = "|[().\\^$?+*{";
        for (int i = 0; i < literal.length(); i++) {
            char c = literal.charAt(i);
            if (c == '\n') {
                result.append("\\n");
            } else if (c == '\t') {
                result.append("\\t");
            } else if (c < ' ' || c > '~') {
                appendUnicodeEscape(result, c);
            } else {
                if (REGEXP_META_CHARACTERS.indexOf(c) != -1) {
                    result.append('\\');
                }
                result.append(c);
            }
        }
        return result.toString();
    }
    
    /**
     * Returns the length in characters of the prefix common to
     * both s1 and s2. If the strings start with different characters,
     * the result is 0.
     */
    public static int lengthOfCommonPrefix(CharSequence s1, CharSequence s2) {
        int i = 0;
        for (; i < s1.length() && i < s2.length(); i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return i;
            }
        }
        return i;
    }
    
    /**
     * Joins the strings in 'strings' with 'separator' between each.
     */
    public static String join(CharSequence[] strings, CharSequence separator) {
        StringBuilder result = new StringBuilder();
        for (CharSequence string : strings) {
            if (result.length() > 0) {
                result.append(separator);
            }
            result.append(string);
        }
        return result.toString();
    }
    
    /**
     * Joins the strings in 'strings' with 'separator' between each.
     */
    public static String join(List strings, CharSequence separator) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < strings.size(); ++i) {
            if (i > 0) {
                result.append(separator);
            }
            result.append(strings.get(i));
        }
        return result.toString();
    }

    /**
     * Returns a copy of the string, with leading whitespace
     * omitted. See String.trim.
     */
    public static String trimLeadingWhitespace(String s) {
        final int length = s.length();
        int i = 0;
        while (i < length && s.charAt(i) <= ' ') {
            ++i;
        }
        return s.substring(i);
    }
    
    /**
     * Returns a copy of the string, with trailing whitespace
     * omitted. See String.trim.
     */
    public static String trimTrailingWhitespace(String s) {
        final int length = s.length();
        int i = length;
        while (i > 0 && s.charAt(i - 1) <= ' ') {
            --i;
        }
        return s.substring(0, i);
    }

    /**
     * A convenient wrapper around URLEncoder.encode.
     */
    public static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // Can't happen. We're guaranteed UTF-8.
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Returns the number of times 'ch' occurs in 'chars'.
     */
    public static int count(CharSequence chars, char ch) {
        int result = 0;
        for (int i = 0; i < chars.length(); ++i) {
            if (chars.charAt(i) == ch) {
                ++result;
            }
        }
        return result;
    }
    
    /**
     * Tests whether 'chars' contains at least one instance of 'ch'.
     */
    public static boolean contains(CharSequence chars, char ch) {
        for (int i = 0; i < chars.length(); ++i) {
            if (chars.charAt(i) == ch) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * See java.lang.String.indexOf.
     */
    public static int indexOf(CharSequence haystack, CharSequence needle, int fromIndex) {
        // This code is the same as that implementing String and StringBuilder's indexOf, only CharSequence rather than char[].
        // We preserve the full generality, but don't use it.
        // The only changes below this block of declarations is to use charAt rather than [].
        final CharSequence source = haystack;
        final CharSequence target = needle;
        final int sourceOffset = 0;
        final int targetOffset = 0;
        final int sourceCount = haystack.length();
        final int targetCount = needle.length();
        
        if (fromIndex >= sourceCount) {
            return (targetCount == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (targetCount == 0) {
            return fromIndex;
        }
        
        char first = target.charAt(targetOffset);
        int max = sourceOffset + (sourceCount - targetCount);
        
        for (int i = sourceOffset + fromIndex; i <= max; i++) {
            /* Look for first character. */
            if (source.charAt(i) != first) {
                while (++i <= max && source.charAt(i) != first);
            }
            
            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = targetOffset + 1; j < end && source.charAt(j) == target.charAt(k); j++, k++);
                
                if (j == end) {
                    /* Found whole string. */
                    return i - sourceOffset;
                }
            }
        }
        return -1;
    }
    
    public static String stackTraceFromThrowable(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
    
    /**
     * Returns a string consisting of 'count' copies of 'ch'.
     */
    public static String nCopies(int count, char ch) {
        String charAsString = Character.toString(ch);
        if (count == 1) {
            return charAsString;
        }
        return nCopies(count, charAsString);
    }
    
    /**
     * Returns a string consisting of 'count' copies of 's'.
     */
    public static String nCopies(int count, CharSequence s) {
        if (count == 1) {
            return s.toString();
        }
        StringBuilder builder = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; ++i) {
            builder.append(s);
        }
        return builder.toString();
    }
    
    public static String pluralize(int value, CharSequence singularForm, CharSequence pluralForm) {
        return Integer.toString(value) + " " + (value == 1 ? singularForm : pluralForm);
    }
    
    private StringUtilities() {
    }

    public static void main(String[] arguments) {
        System.out.println("'" + trimLeadingWhitespace("  hello world  ") + "'");
        System.out.println("'" + trimTrailingWhitespace("  hello world  ") + "'");
        System.out.println("blah="+escapeForJava("hello\tworld\u01f8\n"));
        System.out.println("blah2="+escapeForJava(unescapeJava(escapeForJava("hello\tworld\u01f8\n"))));
        for (String argument : arguments) {
            String escaped = escapeForJava(argument);
            System.out.println("escaped="+escaped);
            System.out.println("unescaped="+unescapeJava(escaped));
        }
        
        Stopwatch op1Stopwatch = Stopwatch.get("nCopies(1, ' ')");
        for (int i = 0; i < 1000000; ++i) {
            Stopwatch.Timer timer = op1Stopwatch.start();
            try {
                nCopies(1, " ");
            } finally {
                timer.stop();
            }
        }
        System.err.println(op1Stopwatch);
        
        Stopwatch op2Stopwatch = Stopwatch.get("nCopies(1, \" \")");
        for (int i = 0; i < 1000000; ++i) {
            Stopwatch.Timer timer = op2Stopwatch.start();
            try {
                nCopies(1, " ");
            } finally {
                timer.stop();
            }
        }
        System.err.println(op2Stopwatch);
    }
}
