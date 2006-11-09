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
        
        // FIXME: we probably need code similar to what we have in the Java styler to recognize bad/unclosed entities.
        textArea.addStyleApplicator(new RegularExpressionStyleApplicator(textArea, "(&.*?;)", PStyle.KEYWORD));
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
