package e.ptextarea;

import java.util.*;

public class PTalcTextStyler extends PAbstractLanguageStyler {
    private static final String[] KEYWORDS = new String[] {
        // "man talc"
        "break",
        "class",
        "continue",
        "do",
        "else",
        "false",
        "final",
        "for",
        "function",
        "if",
        "new",
        "null",
        "return",
        "true",
        "void",
        "while"
    };
    
    public PTalcTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return isShellComment(line, atIndex);
    }
    
    @Override
    protected boolean supportMultiLineComments() {
        return false;
    }
    
    public void addKeywordsTo(Collection<String> collection) {
        collection.addAll(Arrays.asList(KEYWORDS));
    }
}
