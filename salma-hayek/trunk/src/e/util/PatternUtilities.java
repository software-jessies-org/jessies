package e.util;

import java.util.regex.*;

public final class PatternUtilities {
    /**
     * Compiles the given regular expression into a Pattern that may or may
     * not be case-sensitive, depending on the regular expression. If the
     * regular expression contains any capital letter, that is assumed to be
     * meaningful, and the resulting Pattern is case-sensitive. If the whole
     * regular expression is lower-case, the resulting Pattern is
     * case-insensitive.
     * 
     * This is useful for implementing functionality like emacs/vim's "smart
     * case", hence the name.
     */
    public static Pattern smartCaseCompile(String regularExpression) {
        boolean caseInsensitive = true;
        for (int i = 0; i < regularExpression.length(); ++i) {
            if (Character.isUpperCase(regularExpression.charAt(i))) {
                caseInsensitive = false;
            }
        }
        return Pattern.compile(regularExpression, caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
    }
    
    /**
     * Pattern.pattern and Pattern.toString ignore any flags supplied to
     * Pattern.compile, so the regular expression you get out doesn't
     * correspond to what the Pattern was actually matching. This fixes that.
     * 
     * Note that there are some flags that can't be represented.
     * 
     * FIXME: why don't we use Pattern.LITERAL instead of home-grown escaping
     * code? Is it because you can't do the reverse transformation? Should we
     * integrate that code with this?
     */
    public static String toString(Pattern pattern) {
        String regex = pattern.pattern();
        final int flags = pattern.flags();
        if (flags != 0) {
            StringBuilder builder = new StringBuilder("(?");
            toStringHelper(builder, flags, Pattern.UNIX_LINES, 'd');
            toStringHelper(builder, flags, Pattern.CASE_INSENSITIVE, 'i');
            toStringHelper(builder, flags, Pattern.COMMENTS, 'x');
            toStringHelper(builder, flags, Pattern.MULTILINE, 'm');
            toStringHelper(builder, flags, Pattern.DOTALL, 's');
            toStringHelper(builder, flags, Pattern.UNICODE_CASE, 'u');
            builder.append(")");
            regex = builder.toString() + regex;
        }
        return regex;
    }
    
    // @see toString
    private static void toStringHelper(StringBuilder result, int flags, int flag, char c) {
        if ((flags & flag) != 0) {
            result.append(c);
        }
    }
    
    private PatternUtilities() {
    }
}
