package e.ptextarea;

public class PLuaIndenter extends PGenericIndenter {
    private static final String INDENT_AFTER = "\\b(repeat|else|do|then)\\s*$\\b|function.*\\)|[\\(\\[\\{]$";
    private static final String INDENT = null;
    private static final String UNINDENT = "^\\s*((end|until|else|elseif)\\b|[\\)\\]\\}])";
    private static final String ELECTRICS = "endutilsf])}";
    
    public PLuaIndenter(PTextArea textArea) {
        super(textArea, INDENT_AFTER, INDENT, UNINDENT, ELECTRICS);
    }
}
