package e.ptextarea;

public class PBashIndenter extends PGenericIndenter {
    private static final String INDENT_AFTER = "(\\{(?![^\\}]*\\})|\\b(then|elif|else)\\b(?!.*\\bfi\\b)|\\bdo\\b(?!.+\\b(done)\\b)|\\b(case)\\s+\\S+\\s+in\\b(?!.*\\besac\\b)|\\[\\[)";
    private static final String INDENT = "\\$\\{.*\\}";
    private static final String UNINDENT = "(\\}|\\b(fi|elif|else)\\b|\\b(done)\\b|\\b(esac)\\b|\\]\\])";
    private static final String ELECTRICS = "{}cefin";
    
    public PBashIndenter(PTextArea textArea) {
        super(textArea, INDENT_AFTER, INDENT, UNINDENT, ELECTRICS);
    }
}
