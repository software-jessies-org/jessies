package e.ptextarea;

public class PTalcTextStyler extends PAbstractLanguageStyler {
    public PTalcTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return isShellComment(line, atIndex) || line.startsWith("//", atIndex);
    }
    
    @Override protected boolean supportMultiLineComments() {
        return true;
    }
    
    public String[] getKeywords() {
        return new String[] {
            // "man talc" keywords.
            "assert",
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
            "import",
            "in",
            "new",
            "null",
            "return",
            "static",
            "true",
            "void",
            "while",
            
            // "man talc" types.
            "bool",
            "file",
            "int",
            "list",
            "map",
            "match",
            "object",
            "real",
            "string"
        };
    }
}
