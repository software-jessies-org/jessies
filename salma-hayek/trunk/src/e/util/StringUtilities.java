package e.util;

import java.io.*;
import java.util.*;

public class StringUtilities {
    /** Reads all the lines from the named file into a string array. Throws a RuntimeException on failure. */
    public static String[] readLinesFromFile(String filename) {
        ArrayList result = new ArrayList();
        LineNumberReader in = null;
        try {
            File file = FileUtilities.fileFromString(filename);
            in = new LineNumberReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while ((line = in.readLine()) != null) {
                result.add(line);
            }
            return (String[]) result.toArray(new String[result.size()]);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            FileUtilities.close(in);
        }
    }
    
    /** Reads the entire contents of the named file into a string. Throws a RuntimeException on failure. */
    public static String readFile(String filename) {
        StringBuffer result = new StringBuffer();
        String[] lines = readLinesFromFile(filename);
        for (int i = 0; i < lines.length; i++) {
            result.append(lines[i]);
            result.append('\n');
        }
        return result.toString();
    }
    
    /** Writes the given String to the given File. Returns the text of the exception message on failure, null on success. */
    public static String writeFile(File file, String content) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileOutputStream(file));
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
        StringBuffer result = new StringBuffer();
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
        StringBuffer result = new StringBuffer();
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
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    /** Converts a string into a regular expression matching exactly that string. */
    public static String regularExpressionFromLiteral(String literal) {
        StringBuffer result = new StringBuffer();
        final String REGEXP_META_CHARACTERS = "|[().\\^$?+*{";
        for (int i = 0; i < literal.length(); i++) {
            char c = literal.charAt(i);
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
     * Returns the length in characters of the directory prefix common
     * to both s1 and s2. This differs from the normal prefix in that
     * characters past a directory separator don't count. So the result
     * for "/s/apple" and "/s/apricot" does not include the "ap" they
     * have in common. This is intended to be used for finding out the
     * common ancestor of files in the directory hierarchy.
     */
    public static int lengthOfCommonDirectoryPrefix(String s1, String s2) {
        int result = lengthOfCommonPrefix(s1, s2);
        result = s1.lastIndexOf(File.separator, result);
        return result;
    }
    
    public static String join(String[] strings, String separator) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < strings.length; ++i) {
            result.append(strings[i]);
            result.append(separator);
        }
        return result.toString();
    }

    private StringUtilities() {
    }

    public static void main(String[] args) {
        System.out.println("blah="+escapeForJava("hello\tworld\u01f8\n"));
        System.out.println("blah2="+escapeForJava(unescapeJava(escapeForJava("hello\tworld\u01f8\n"))));
        for (int i = 0; i < args.length; i++) {
            String escaped = escapeForJava(args[i]);
            System.out.println("escaped="+escaped);
            System.out.println("unescaped="+unescapeJava(escaped));
        }
    }
}
