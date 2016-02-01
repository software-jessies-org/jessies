package e.util;

import e.gui.*;
import java.awt.*;
import java.util.regex.*;
import javax.swing.*;
import org.jessies.test.*;

public final class PatternUtilities {
    /**
     * The URL of our recommended documentation about regular expressions.
     * Link to this in UI.
     * @see addRegularExpressionHelpToComponent
     */
    public static final String DOCUMENTATION_URL = "http://developer.android.com/reference/java/util/regex/Pattern.html";
    
    // This character class and the regular expression below are based on the BNF in RFC 1738.
    // Compromises have been made to fit the grammar into a fairly readable regular expression.
    // If we needed to, I think we could write an exact regular expression.
    private static final String SEARCH_CHARS = "[/A-Za-z0-9;:@&=%!*'(),$_.+-]";
    
    /**
     * 
     */
    public static final Pattern HYPERLINK_PATTERN = Pattern.compile("\\b(https?://[A-Za-z0-9.:-]+[A-Za-z0-9](/~?"+SEARCH_CHARS+"*(\\?"+SEARCH_CHARS+"*)?)?(\\#"+SEARCH_CHARS+"+)?)(?<![),.])");
    
    @Test private static void testHyperlinkPattern() {
        Assert.matches(HYPERLINK_PATTERN, "pre http://software.jessies.org post", "http://software.jessies.org");
        Assert.matches(HYPERLINK_PATTERN, "pre http://software.jessies.org/ post", "http://software.jessies.org/");
        Assert.matches(HYPERLINK_PATTERN, "pre http://software.jessies.org/software/make/manual/html_mono/make.html post", "http://software.jessies.org/software/make/manual/html_mono/make.html");
        Assert.matches(HYPERLINK_PATTERN, "pre http://software.jessies.org/viewcvs/gtk%2B/gtk/gtkstock.h?view=markup post", "http://software.jessies.org/viewcvs/gtk%2B/gtk/gtkstock.h?view=markup");
        Assert.matches(HYPERLINK_PATTERN, "<a href=\"http://software.jessies.org\">Software Jessies</a>", "http://software.jessies.org");
        Assert.matches(HYPERLINK_PATTERN, "pre http://software.jessies.org post", "http://software.jessies.org");
        Assert.matches(HYPERLINK_PATTERN, "(http://software.jessies.org)", "http://software.jessies.org");
        Assert.matches(HYPERLINK_PATTERN, "(http://software.jessies.org/)", "http://software.jessies.org/");
        Assert.matches(HYPERLINK_PATTERN, "<http://software.jessies.org>", "http://software.jessies.org");
        Assert.matches(HYPERLINK_PATTERN, "http://software.jessies.org, http://software.jessies.org.", "http://software.jessies.org", "http://software.jessies.org");
        Assert.matches(HYPERLINK_PATTERN, "http://software.jessies.org/, http://software.jessies.org/.", "http://software.jessies.org/", "http://software.jessies.org/");
        Assert.matches(HYPERLINK_PATTERN, "pre http://software.jessies.org/~user/ post", "http://software.jessies.org/~user/");
        Assert.matches(HYPERLINK_PATTERN, "pre http://software.jessies.org/~user/page.html#target post", "http://software.jessies.org/~user/page.html#target");
    }
    
    /**
     * Compiles the given regular expression into a Pattern that may or may not be case-sensitive, depending on the regular expression.
     * If the regular expression contains any capital letter, that is assumed to be meaningful, and the resulting Pattern is case-sensitive.
     * If the whole regular expression is lower-case, the resulting Pattern is case-insensitive.
     * 
     * This is useful for implementing functionality like emacs/vim's "smart case", hence the name.
     * 
     * By default, we enable (?m) on the assumption that it's more useful if ^ and $ match the start and end of each line rather than just the start and end of input.
     */
    public static Pattern smartCaseCompile(String regularExpression) {
        boolean caseInsensitive = true;
        for (int i = 0; i < regularExpression.length(); ++i) {
            if (Character.isUpperCase(regularExpression.charAt(i))) {
                caseInsensitive = false;
            }
        }
        int flags = Pattern.MULTILINE;
        if (caseInsensitive) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        return Pattern.compile(regularExpression, flags);
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
    
    /**
     * Most users don't know everything there is to know about regular expressions, so it's nice to offer them a link to the documentation.
     * A convenient place to do this is the status bar.
     * It's a bad idea to not have a status bar for a dialog that takes a regular expression, because you need somewhere to report syntax errors.
     * For very small dialogs with a single field, though, it might make sense to have the help next to the field.
     */
    public static JPanel addRegularExpressionHelpToComponent(Component component) {
        // Use a shorter title if it looks like we're trying to save space by fitting next to a field rather than in a status line.
        String label = (component instanceof JTextField) ? "Help" : "Regular Expression Help";
        JPanel result = new JPanel(new BorderLayout());
        result.add(component, BorderLayout.CENTER);
        result.add(new JHyperlinkButton(label, DOCUMENTATION_URL), BorderLayout.EAST);
        return result;
    }
    
    private PatternUtilities() {
    }
}
