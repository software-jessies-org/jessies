package e.ptextarea;

import e.gui.*;
import e.util.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Links to a bug database from check-in comments.
 * 
 * Examples:
 *   Sun Java bug parade: Sun 6227617. Bug id 6227617. Bug 6227617.
 *   RFCs: RFC2229.
 */
public class BugDatabaseHighlighter extends RegularExpressionStyleApplicator {
    private String urlTemplate;
    
    private BugDatabaseHighlighter(PTextArea textArea, String regularExpression, String urlTemplate) {
        super(textArea, regularExpression, PStyle.HYPERLINK);
        this.urlTemplate = urlTemplate;
    }
    
    public static void highlightBugs(PTextArea textArea) {
        // Group 1 - the text to be underlined.
        // Group 2 - the id, inserted into the template.
        
        // Sun Java bugs.
        highlightBug(textArea, "\\b(([4-6]\\d{6}))\\b", "http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=%s");
        
        // RFCs.
        highlightBug(textArea, "(?i)\\b(rfc\\s*(\\d{3,4}))\\b", "http://ftp.rfc-editor.org/in-notes/rfc%s");
        
        // BlueArc's internal bug database.
        highlightBug(textArea, "\\b(D([1-2]\\d{4}))\\b", "http://woggle/%s");
    }
    
    private static void highlightBug(PTextArea textArea, String regularExpression, String urlTemplate) {
        textArea.addStyleApplicator(new BugDatabaseHighlighter(textArea, regularExpression, urlTemplate));
    }
    
    @Override
    public boolean canApplyStylingTo(PStyle style) {
        return (style == PStyle.NORMAL || style == PStyle.COMMENT);
    }
    
    private String urlForMatcher(Matcher matcher) {
        return new Formatter().format(urlTemplate, matcher.group(2)).toString();
    }
    
    @Override
    protected void configureSegment(PTextSegment segment, Matcher matcher) {
        String url = urlForMatcher(matcher);
        segment.setLinkAction(new BugLinkActionListener(url));
        segment.setToolTip(url);
    }
    
    private static class BugLinkActionListener implements ActionListener {
        private String url;
        
        public BugLinkActionListener(String url) {
            this.url = url;
        }
        
        public void actionPerformed(ActionEvent e) {
            try {
                BrowserLauncher.openURL(url);
            } catch (IOException ex) {
                SimpleDialog.showDetails(null, "Bug Database Link", ex);
            }
        }
    }
}
