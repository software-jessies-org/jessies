package e.ptextarea;


/**
 * A PCPPTextStyler knows how to apply syntax highlighting for C++ code.
 * 
 * @author Phil Norman
 */

public class PCPPTextStyler extends PCTextStyler {
    private static final String[] CPP_KEYWORDS = new String[] {
        "bool", "catch", "class", "delete", "friend", "inline",
        "new", "namespace", "operator", "private", "protected", "public",
        "template", "this", "throw", "try",
        
        // Technically these are literals, not keywords, but they're so widely used they're worth including.
        "true", "false",
    };
    
    public PCPPTextStyler(PTextArea textArea) {
        super(textArea);
        addKeywords(CPP_KEYWORDS);
    }
}
