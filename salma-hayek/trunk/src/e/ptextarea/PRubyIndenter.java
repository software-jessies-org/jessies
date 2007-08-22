package e.ptextarea;

import java.util.regex.*;

public class PRubyIndenter extends PGenericIndenter {
    private static final String INDENT_AFTER = "(^\\s*(begin|case|catch|class|def|do|else|elsif|ensure|for|if|module|rescue|when|while|unless|until)\\b(?!.*\\b(end)\\b)|\\{(?![^}]*})|\\b(do)\\b)";
    
    private static final String INDENT = null;
    
    private static final String UNINDENT = "\\b(end|else|elsif|when|ensure|rescue)\\b|^[^{]*}";
    
    /**
     * The final characters in "end", "else", "elsif", "when", "ensure", "rescue" and "}".
     * Plus "s", for when you turn an "if" into an "elsif".
     * Is indenting Ruby efficient enough that we can just do it on every change?
     */
    private static final String ELECTRICS = "defns}";
    
    public PRubyIndenter(PTextArea textArea) {
        super(textArea, INDENT_AFTER, INDENT, UNINDENT, ELECTRICS);
    }
}
