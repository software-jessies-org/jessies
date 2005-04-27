package e.ptextarea;


/**
 * A PCTextStyler knows how to apply syntax highlighting for C code.
 * 
 * @author Phil Norman
 */

public class PCTextStyler extends PCLikeTextStyler {
    
    private static final String[] KEYWORDS = new String[] {
        "auto", "break", "case", "char", "const", "continue", "default", "do",
        "double", "else", "enum", "extern", "float", "for", "goto", "if",
        "int", "long", "register", "return", "short", "signed", "static", "struct",
        "switch", "typedef", "union", "unsigned", "void", "volatile", "while"
    };
    
    public PCTextStyler(PTextArea textArea) {
        super(textArea);
        addKeywords(KEYWORDS);
    }
    
    public boolean supportShellComments() {
        return false;
    }

    public boolean supportDoubleSlashComments() {
        return true;
    }
}
