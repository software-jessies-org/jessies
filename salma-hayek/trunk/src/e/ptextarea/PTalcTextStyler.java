package e.ptextarea;

import java.util.*;

public class PTalcTextStyler extends PAbstractLanguageStyler {
    private static final String[] KEYWORDS = new String[] {
        // "man talc" keywords.
        "break",
        "class",
        "continue",
        "do",
        "else",
        "extends",
        "false",
        "final",
        "for",
        "function",
        "if",
        "implements",
        "in",
        "new",
        "null",
        "return",
        "true",
        "void",
        "while",
        
        // "man talc" types.
        "bool",
        "file",
        "int",
        "match",
        "object",
        "real",
        "string"
    };
    
    public PTalcTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return isShellComment(line, atIndex) || line.startsWith("//", atIndex);
    }
    
    @Override
    protected boolean supportMultiLineComments() {
        return true;
    }
    
    public void addKeywordsTo(Collection<String> collection) {
        collection.addAll(Arrays.asList(KEYWORDS));
    }
}
