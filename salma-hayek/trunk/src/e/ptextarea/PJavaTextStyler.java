package e.ptextarea;


/**
 * A PJavaTextStyler knows how to apply syntax highlighting for Java code.
 * 
 * @author Phil Norman
 */

public class PJavaTextStyler extends PCLikeTextStyler {
    
    private static final String[] KEYWORDS = new String[] {
        // Keywords as defined in the java language specification.
        "abstract", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
        "default", "do", "double", "else", "extends", "final", "finally", "float", "for", "goto",
        "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package",
        "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized",
        "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
        
        // Technically these are literals, not keywords, but I'll treat them the same way anyway.
        "true", "false", "null",
    };
    
    public PJavaTextStyler(PTextArea textArea) {
        super(textArea);
        addKeywords(KEYWORDS);
    }
    
    public boolean supportShellComments() {
        return false;
    }
}
