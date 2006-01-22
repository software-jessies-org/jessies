package e.ptextarea;

import e.gui.*;
import e.util.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Links to web sites from written-out URLs.
 * 
 * Examples:
 *   http://www.google.com
 *   http://www.google.com/
 *   http://www.gnu.org/software/make/manual/html_mono/make.html
 *   http://cvs.gnome.org/viewcvs/gtk%2B/gtk/gtkstock.h?view=markup
 *   <a href="http://www.google.com">Google</a>
 *   "http://www.google.com"
 *   (http://www.google.com)
 *   <http://www.google.com>
 *   http://www.google.com, http://www.google.com.
 */
class HyperlinkStyleApplicator extends RegularExpressionStyleApplicator {
    // This character class and the regular expression below are based on the BNF in RFC 1738.
    // Compromises have been made to fit the grammar into a fairly readable regular expression.
    // If we needed to, I think we could write an exact regular expression.
    private static final String SEARCH_CHARS = "[/A-Za-z0-9;:@&=%!*'(),$_.+-]";
    
    public HyperlinkStyleApplicator(PTextArea textArea) {
        super(textArea, "\\b(https?://[A-Za-z0-9.:-]+[A-Za-z0-9](/"+SEARCH_CHARS+"*(\\?"+SEARCH_CHARS+"*)?)?)", PStyle.HYPERLINK);
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
}
