package e.ptextarea;

import java.util.*;

/**
 * Applies colors to XML/HTML derivatives' source.
 */
public class PXmlTextStyler extends PAbstractLanguageStyler {
    public PXmlTextStyler(PTextArea textArea) {
        super(textArea);
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
