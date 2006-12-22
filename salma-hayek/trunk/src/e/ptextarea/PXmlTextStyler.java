package e.ptextarea;

import java.util.*;

/**
 * Applies colors to XML/HTML derivatives' source.
 */
public class PXmlTextStyler extends PAbstractLanguageStyler {
    public PXmlTextStyler(PTextArea textArea) {
        super(textArea);
        
        // We add the tag-recognizing applicator first so that the standard hyperlink applicator doesn't interfere with it.
        // FIXME: we don't cope with tags split across multiple lines because our styling works on a per-line basis; we'd need to remember state like we do for multi-line comments.
        textArea.addStyleApplicatorFirst(new RegularExpressionStyleApplicator(textArea, "(<.*?>)", PStyle.KEYWORD));
        
        // Recognize lexically plausible entities (though we don't actually check that they're known entities in any particular DTD).
        // See http://www.w3.org/TR/REC-xml/ for more detail.
        String hexEscape = "(#[xX]\\p{XDigit}+)";
        String decimalEscape = "(#\\d+)";
        String escape = "(" + hexEscape + "|" + decimalEscape + ")";
        String entityName = "([\\p{L}[\\p{N}]]*)";
        String entityTail = "(" + escape + "|" + entityName + ")" + ";"; // "Everything after the &".
        textArea.addStyleApplicator(new RegularExpressionStyleApplicator(textArea, "(&" + entityTail + ")", PStyle.KEYWORD));
        // And mark implausible entities as errors.
        textArea.addStyleApplicator(new RegularExpressionStyleApplicator(textArea, "(&(?!" + entityTail + ")\\S*)", PStyle.ERROR));
    }
    
    @Override
    protected boolean supportMultiLineComments() {
        return true;
    }
    
    @Override
    protected String multiLineCommentStart() {
        return "<!--";
    }
    
    @Override
    protected String multiLineCommentEnd() {
        return "-->";
    }
    
    @Override
    protected boolean isQuote(char ch) {
        return false;
    }
    
    public void addKeywordsTo(Collection<String> collection) {
    }
}
