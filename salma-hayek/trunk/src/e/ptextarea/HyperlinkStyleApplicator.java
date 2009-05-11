package e.ptextarea;

import e.gui.WebLinkAction;
import e.util.PatternUtilities;
import java.util.regex.Matcher;

/**
 * Links to web sites from written-out URLs.
 * 
 * See test below for example URLs.
 */
class HyperlinkStyleApplicator extends RegularExpressionStyleApplicator {
    public HyperlinkStyleApplicator(PTextArea textArea) {
        super(textArea, PatternUtilities.HYPERLINK_PATTERN, PStyle.HYPERLINK);
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
