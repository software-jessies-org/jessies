package e.ptextarea;

import java.util.*;

public class PJavaScriptTextStyler extends PAbstractLanguageStyler {
    private static final String[] KEYWORDS = new String[] {
        // http://developer.mozilla.org/en/docs/Core_JavaScript_1.5_Reference:Reserved_Words
        "break",
        "case",
        "catch",
        "continue",
        "default",
        "delete",
        "do",
        "else",
        "finally",
        "for",
        "function",
        "if",
        "in",
        "instanceof",
        "new",
        "return",
        "switch",
        "this",
        "throw",
        "try",
        "typeof",
        "var",
        "void",
        "while",
        "with",
        "const",
        "export",
        "import",
        // http://developer.mozilla.org/en/docs/New_in_JavaScript_1.7
        "let",
        "yield"
    };
    
    public PJavaScriptTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return line.startsWith("//", atIndex);
    }
    
    @Override
    protected boolean supportMultiLineComments() {
        return true;
    }
    
    @Override
    protected boolean isQuote(char ch) {
        return (ch == '\'' || ch == '\"');
    }
    
    public void addKeywordsTo(Collection<String> collection) {
        collection.addAll(Arrays.asList(KEYWORDS));
    }
}
