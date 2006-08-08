package e.ptextarea;

import java.util.*;

public class PPythonTextStyler extends PAbstractLanguageStyler {
    private static final String[] KEYWORDS = new String[] {
        // python -c 'import keyword ; print keyword.kwlist'
        "and",
        "assert",
        "break",
        "class",
        "continue",
        "def",
        "del",
        "elif",
        "else",
        "except",
        "exec",
        "finally",
        "for",
        "from",
        "global",
        "if",
        "import",
        "in",
        "is",
        "lambda",
        "not",
        "or",
        "pass",
        "print",
        "raise",
        "return",
        "try",
        "while",
        "yield",
    };
    
    public PPythonTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    protected boolean isCommentToEndOfLineStart(String line, int atIndex) {
        return isShellComment(line, atIndex);
    }
    
    @Override
    protected boolean supportSlashStarComments() {
        return false;
    }
    
    @Override
    protected boolean isQuote(char ch) {
        return (ch == '\'' || ch == '\"' || ch == '`');
    }
    
    public void addKeywordsTo(Collection<String> collection) {
        collection.addAll(Arrays.asList(KEYWORDS));
    }
}
