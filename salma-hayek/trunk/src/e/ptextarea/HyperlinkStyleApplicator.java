package e.ptextarea;

import e.gui.WebLinkAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jessies.test.Assert;
import org.jessies.test.Test;
import org.jessies.test.TestHelper;

/**
 * Links to web sites from written-out URLs.
 * 
 * See test below for example URLs.
 */
class HyperlinkStyleApplicator extends RegularExpressionStyleApplicator {
    // This character class and the regular expression below are based on the BNF in RFC 1738.
    // Compromises have been made to fit the grammar into a fairly readable regular expression.
    // If we needed to, I think we could write an exact regular expression.
    private static final String SEARCH_CHARS = "[/A-Za-z0-9;:@&=%!*'(),$_.+-]";
    
    private static final Pattern LINK_PATTERN = Pattern.compile("\\b(https?://[A-Za-z0-9.:-]+[A-Za-z0-9](/~?"+SEARCH_CHARS+"*(\\?"+SEARCH_CHARS+"*)?)?(\\#"+SEARCH_CHARS+"+)?)(?<![),.])");
    
    public HyperlinkStyleApplicator(PTextArea textArea) {
        super(textArea, LINK_PATTERN, PStyle.HYPERLINK);
    }
    
    @Override
    public boolean canApplyStylingTo(PStyle style) {
        return (style == PStyle.NORMAL || style == PStyle.COMMENT);
    }
    
    @Override
    protected void configureSegment(PTextSegment segment, Matcher matcher) {
        String url = matcher.group(1);
        segment.setLinkAction(new WebLinkAction("Web Link", url));
    }
    
    @Test private static void testExamples() {
        assertMatches("http://software.jessies.org");
        assertMatches("http://software.jessies.org/");
        assertMatches("http://software.jessies.org/software/make/manual/html_mono/make.html");
        assertMatches("http://software.jessies.org/viewcvs/gtk%2B/gtk/gtkstock.h?view=markup");
        assertMatches("<a href=\"http://software.jessies.org\">Software Jessies</a>", "http://software.jessies.org");
        assertMatches("http://software.jessies.org");
        assertMatches("(http://software.jessies.org)", "http://software.jessies.org");
        assertMatches("(http://software.jessies.org/)", "http://software.jessies.org/");
        assertMatches("<http://software.jessies.org>", "http://software.jessies.org");
        assertMatches("http://software.jessies.org, http://software.jessies.org.", "http://software.jessies.org", "http://software.jessies.org");
        assertMatches("http://software.jessies.org/, http://software.jessies.org/.", "http://software.jessies.org/", "http://software.jessies.org/");
        assertMatches("http://software.jessies.org/~user/");
        assertMatches("http://software.jessies.org/~user/page.html#target");
    }
    
    @TestHelper private static void assertMatches(final String text) {
        assertMatches(text, text);
    }
    
    @TestHelper private static void assertMatches(final String text, final String... expectedUrls) {
        final List<String> expected = Arrays.asList(expectedUrls);
        final List<String> actual = new ArrayList<String>();
        final Matcher matcher = LINK_PATTERN.matcher(text);
        while (matcher.find()) {
            actual.add(matcher.group(1));
        }
        Assert.equals(expected, actual);
    }
}
