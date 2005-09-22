package e.util;

import java.io.*;
import java.util.*;

public class StringUtilities {
    /** Reads all the lines from the named file into a string array. Throws a RuntimeException on failure. */
    public static String[] readLinesFromFile(String filename) {
        ArrayList<String> result = new ArrayList<String>();
        LineNumberReader in = null;
        try {
            File file = FileUtilities.fileFromString(filename);
            in = new LineNumberReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result.add(line);
            }
            return result.toArray(new String[result.size()]);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            FileUtilities.close(in);
        }
    }
    
    /** Reads the entire contents of the named file into a String. Throws a RuntimeException on failure. */
    public static String readFile(String filename) {
        StringBuilder result = new StringBuilder();
        String[] lines = readLinesFromFile(filename);
        for (String line : lines) {
            result.append(line);
            result.append('\n');
        }
        return result.toString();
    }
    
    /** Reads the entire contents of the given File into a String. Throws a RuntimeException on failure. */
    public static String readFile(File file) {
        return readFile(file.toString());
    }
    
    /** Writes the given String to the given File. Returns the text of the exception message on failure, null on success. */
    public static String writeFile(File file, String content) {
        return writeFile(file, content, false);
    }
    
    /** Appends the given String to the given File. Returns the text of the exception message on failure, null on success. */
    public static String appendToFile(File file, String content) {
        return writeFile(file, content, true);
    }
    
    private static String writeFile(File file, String content, boolean append) {
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
    
    /** Used by the escape method to pad Unicode escapes to four digits. */
    private static final char[] ZEROES = { '0', '0', '0' };
    
    /** Turns a string into a printable Java string literal (minus the quotes). So a tab is converted to "\t", et cetera. */
    public static String escapeForJava(String s) {
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
                result.append("\\u");
                String digits = Integer.toString((int) c, 16);
                result.append(ZEROES, 0, 4 - digits.length());
                result.append(digits);
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
    public static String regularExpressionFromLiteral(String literal) {
        StringBuilder result = new StringBuilder();
        final String REGEXP_META_CHARACTERS = "|[().\\^$?+*{";
        for (int i = 0; i < literal.length(); i++) {
            char c = literal.charAt(i);
            if (c == '\n') {
                result.append("\\n");
                continue;
            }
            if (c == '\t') {
                result.append("\\t");
                continue;
            }
            if (REGEXP_META_CHARACTERS.indexOf(c) != -1) {
                result.append('\\');
            }
            result.append(c);
        }
        return result.toString();
    }
    
    /**
     * Returns the length in characters of the prefix common to
     * both s1 and s2. If the strings start with different characters,
     * the result is 0.
     */
    public static int lengthOfCommonPrefix(String s1, String s2) {
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
    public static String join(String[] strings, String separator) {
        StringBuilder result = new StringBuilder();
        for (String string : strings) {
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
    public static String join(List strings, String separator) {
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
    
    public static String stackTraceFromThrowable(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
    
    /**
     * Returns a string consisting of 'count' copies of 'ch'.
     */
    public static String nCopies(int count, char ch) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; ++i) {
            builder.append(ch);
        }
        return builder.toString();
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
    }
}
