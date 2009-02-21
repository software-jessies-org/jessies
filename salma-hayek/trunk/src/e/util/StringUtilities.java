package e.util;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.regex.*;
import org.jessies.test.*;

public class StringUtilities {
    /** Reads all the lines from the given file into a string array. Throws a RuntimeException on failure. */
    public static String[] readLinesFromFile(File file) {
        char[] chars = readFileAsCharArray(file);
        // The empty file clearly contains no lines but Java's split (unlike Ruby's) would give us a singleton array containing the empty string.
        if (chars.length == 0) {
            return new String[0];
        }
        return Pattern.compile("\n").split(new CharArrayCharSequence(chars));
    }
    
    /** Reads all the lines from the named file into a string array. Throws a RuntimeException on failure. */
    public static String[] readLinesFromFile(String filename) {
        return readLinesFromFile(FileUtilities.fileFromString(filename));
    }
    
    /** Reads the entire contents of the named file into a String. Throws a RuntimeException on failure. */
    public static String readFile(String filename) {
        return readFile(FileUtilities.fileFromString(filename));
    }
    
    /** Reads the entire contents of the given File into a String. Throws a RuntimeException on failure. */
    public static String readFile(File file) {
        return new String(readFileAsCharArray(file));
    }
    
    private static char[] readFileAsCharArray(File file) {
        try {
            ByteBuffer byteBuffer = ByteBufferUtilities.readFile(file);
            if (ByteBufferUtilities.isBinaryByteBuffer(byteBuffer, byteBuffer.capacity())) {
                throw new RuntimeException("binary file");
            }
            ByteBufferDecoder decoder = new ByteBufferDecoder(byteBuffer, byteBuffer.capacity());
            return decoder.getCharArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Writes the given String to the given File, overwriting any existing content.
     * Returns the text of the exception message on failure, null on success.
     */
    public static String writeFile(File file, CharSequence content) {
        return writeFile0(file, content, null);
    }
    
    /**
     * Writes the given lines to the given File, overwriting any existing content.
     * Returns the text of the exception message on failure, null on success.
     */
    public static String writeFile(File file, List<String> lines) {
        return writeFile0(file, null, lines);
    }
    
    private static String writeFile0(File file, CharSequence content, List<String> lines) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            if (content != null) {
                out.print(content);
            } else {
                for (String line : lines) {
                    // We could use println, but we'd probably want to make readFile smarter first.
                    out.print(line);
                    out.print("\n");
                }
            }
            return null;
        } catch (IOException ex) {
            return ex.getMessage();
        } finally {
            FileUtilities.close(out);
        }
    }
    
    @Test private static void testWriteFile_CharSequence() {
        final File tmpFile = FileUtilities.createTemporaryFile("test", "test file");
        final String expectedString = "hello\nworld";
        Assert.equals(writeFile(tmpFile, expectedString), null);
        Assert.equals(join(readLinesFromFile(tmpFile), "\n"), expectedString);
    }
    
    @Test private static void testWriteFile_List() {
        final File tmpFile = FileUtilities.createTemporaryFile("test", "test file");
        final List<String> expectedLines = Arrays.asList("hello", "world");
        Assert.equals(writeFile(tmpFile, expectedLines), null);
        final List<String> actualLines = Arrays.asList(readLinesFromFile(tmpFile));
        Assert.equals(actualLines, expectedLines);
    }
    
    public static boolean writeAtomicallyTo(File file, CharSequence chars) {
        // We save to a new file first, to reduce our chances of corrupting the real file, or at least increase our chances of having one intact copy.
        File backupFile = new File(file.toString() + ".bak");
        try {
            StringUtilities.writeFile(backupFile, chars);
        } catch (Exception ex) {
            return false;
        }
        
        // Now we write to the intended destination.
        // If the destination was a symbolic link on a CIFS server, it's important to write to the original file rather than creating a new one.
        
        // CIFS also causes problems if we try renaming the backup file to the intended file.
        // For one thing, the destination must not exist, but removing the destination would make it harder to be atomic.
        // Also, the source must not be open, which is not easy to guarantee in Java, and often not the case as soon as you'd like.
        try {
            StringUtilities.writeFile(file, chars);
        } catch (Exception ex) {
            return false;
        }
        
        // Everything went well so far, so delete the backup file (ignoring failures) and return success.
        backupFile.delete();
        return true;
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
        final int sLength = s.length();
        final StringBuilder result = new StringBuilder(sLength);
        for (int i = 0; i < sLength; ++i) {
            final char c = s.charAt(i);
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
        final int sLength = s.length();
        final StringBuilder result = new StringBuilder(sLength);
        for (int i = 0; i < sLength; ++i) {
            final char c = s.charAt(i);
            if (c == '\\' && i < sLength - 1) {
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
    
    @Test private static void testJavaEscaping() {
        final String example = "hello\tworld\u01f8\n";
        Assert.equals(escapeForJava(example), "hello\\tworld\\u01f8\\n");
        Assert.equals(unescapeJava(escapeForJava(example)), example);
    }
    
    /**
     * Escapes &, ", <, and > in the plain-text string 's' so it can be used as HTML.
     */
    public static String escapeForHtml(String s) {
        final int sLength = s.length();
        final StringBuilder result = new StringBuilder(sLength);
        for (int i = 0; i < sLength; ++i) {
            final char ch = s.charAt(i);
            if (ch == '&') {
                result.append("&amp;");
            } else if (ch == '"') {
                result.append("&quot;");
            } else if (ch == '>') {
                result.append("&gt;");
            } else if (ch == '<') {
                result.append("&lt;");
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
    
    /**
     * Reverses the escaping done by escapeForHtml.
     * This method is only meant to cope with HTML from escapeForHtml, not general HTML using arbitrary entities.
     * Unknown entities will be preserved.
     */
    public static String unescapeHtml(String s) {
        final int sLength = s.length();
        final StringBuilder result = new StringBuilder(sLength);
        for (int i = 0; i < sLength; ++i) {
            final char ch = s.charAt(i);
            if (ch == '&') {
                if (s.startsWith("&amp;", i)) {
                    result.append('&');
                    i += 4;
                } else if (s.startsWith("&quot;", i)) {
                    result.append('"');
                    i += 5;
                } else if (s.startsWith("&gt;", i)) {
                    result.append('>');
                    i += 3;
                } else if (s.startsWith("&lt;", i)) {
                    result.append('<');
                    i += 3;
                } else {
                    // There are a lot of entities we don't know. Leave them unmolested.
                    result.append(ch);
                }
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
    
    @Test private static void testEscapeForHtml() {
        Assert.equals(escapeForHtml("hello, world!"), "hello, world!");
        Assert.equals(escapeForHtml("1 < 2 && 2 > 1"), "1 &lt; 2 &amp;&amp; 2 &gt; 1");
        Assert.equals(escapeForHtml("puts(\"hello, world!\")"), "puts(&quot;hello, world!&quot;)");
    }
    
    @Test private static void testUnescapeHtml() {
        Assert.equals(unescapeHtml("hello, world!"), "hello, world!");
        Assert.equals(unescapeHtml("1 &lt; 2 &amp;&amp; 2 &gt; 1"), "1 < 2 && 2 > 1");
        Assert.equals(unescapeHtml("puts(&quot;hello, world!&quot;)"), "puts(\"hello, world!\")");
        Assert.equals(unescapeHtml("hello,&nbsp;world!"), "hello,&nbsp;world!");
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
    
    /** Converts a literal into a form suitable to pass to Matcher's various replacement methods. */
    public static String replacementStringFromLiteral(CharSequence literal) {
        StringBuilder result = new StringBuilder();
        final String REPLACEMENT_META_CHARACTERS = "\\$";
        for (int i = 0; i < literal.length(); i++) {
            char c = literal.charAt(i);
            if (REPLACEMENT_META_CHARACTERS.indexOf(c) != -1) {
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
    public static String join(List<?> strings, CharSequence separator) {
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
    
    @Test private static void testTrimmers() {
        Assert.equals(trimLeadingWhitespace("  hello world  "), "hello world  ");
        Assert.equals(trimTrailingWhitespace("  hello world  "), "  hello world");
    }
    
    /**
     * A convenient wrapper around URLEncoder.encode.
     * Should perhaps have been called escapeForUrl or encodeUrl, so I stop reinventing it.
     */
    public static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // Can't happen. We're guaranteed UTF-8.
            throw new RuntimeException(ex);
        }
    }
    
    public static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // Can't happen. We're guaranteed UTF-8.
            throw new RuntimeException(ex);
        }
    }
    
    @Test private static void testUrlEncode() {
        Assert.equals(urlEncode("hello"), "hello");
        Assert.equals(urlEncode("hello world 1+2"), "hello+world+1%2B2");
        Assert.equals(urlEncode("<hello&world>"), "%3Chello%26world%3E");
    }
    
    @Test private static void testUrlDecode() {
        Assert.equals(urlDecode("hello"), "hello");
        Assert.equals(urlDecode("hello+world+1%2B2"), "hello world 1+2");
        Assert.equals(urlDecode("%3Chello%26world%3E"), "<hello&world>");
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
    
    @Test private static void testNCopies() {
        Assert.equals(nCopies(0, " "), "");
        Assert.equals(nCopies(1, " "), " ");
        Assert.equals(nCopies(8, " "), "        ");
    }
    
    public static String pluralize(int value, CharSequence singularForm, CharSequence pluralForm) {
        return Integer.toString(value) + " " + (value == 1 ? singularForm : pluralForm);
    }
    
    @Test private static void testPluralize() {
        Assert.equals(pluralize(0, "foo", "bar"), "0 bar");
        Assert.equals(pluralize(1, "foo", "bar"), "1 foo");
        Assert.equals(pluralize(8, "foo", "bar"), "8 bar");
    }
    
    private StringUtilities() {
    }
    
    public static void main(String[] arguments) {
        // FIXME: our test framework should support benchmark tests too.
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
