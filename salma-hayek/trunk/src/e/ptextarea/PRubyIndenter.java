package e.ptextarea;

import java.util.regex.*;

public class PRubyIndenter extends PGenericIndenter {
    private static final String INDENT_AFTER = "(^\\s*(begin|case|catch|class|def|else|elsif|ensure|for|if|module|rescue|when|while|unless|until)\\b(?!.*\\b(end)\\b)|\\{(?![^\\}]*\\}))";
    
    private static final String INDENT = null;
    
    private static final String UNINDENT = "\\b(end|else|elsif|when|ensure|rescue)\\b|(<!\\{[^\\{]*)}";
    
    /* The final characters in "end", "else", "elsif", "when", "ensure", "rescue" and "}". */
    private static final String ELECTRICS = "defn}";
    
    public PRubyIndenter(PTextArea textArea) {
        super(textArea, INDENT_AFTER, INDENT, UNINDENT, ELECTRICS);
    }
}
