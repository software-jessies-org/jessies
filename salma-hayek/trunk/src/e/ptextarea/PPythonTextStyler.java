package e.ptextarea;

import java.util.*;

public class PPythonTextStyler extends PAbstractLanguageStyler {
    public PPythonTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return isShellComment(line, atIndex);
    }
    
    @Override protected boolean supportMultiLineComments() {
        return false;
    }
    
    @Override protected boolean isQuote(char ch) {
        return (ch == '\'' || ch == '\"' || ch == '`');
    }
    
    public String[] getKeywords() {
        return new String[] {
            // python -c 'import keyword ; print keyword.kwlist'
            "and",
            "as",
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
            "with",
            "yield",
        };
    }
}
