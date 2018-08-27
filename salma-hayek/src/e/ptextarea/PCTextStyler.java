package e.ptextarea;

/**
 * A PCTextStyler knows how to apply syntax highlighting for C code.
 * 
 * @author Phil Norman
 */
public class PCTextStyler extends PAbstractLanguageStyler {
    public PCTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return line.startsWith("//", atIndex);
    }
    
    @Override protected boolean supportMultiLineComments() {
        return true;
    }
    
    public String[] getKeywords() {
        return new String[] {
            // https://en.cppreference.com/w/c/keyword
            "asm",
            "auto",
            "break",
            "case",
            "char",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extern",
            "float",
            "for",
            "goto",
            "if",
            "inline",
            "int",
            "long",
            "register",
            "restrict",
            "return",
            "short",
            "signed",
            "sizeof",
            "static",
            "struct",
            "switch",
            "typedef",
            "union",
            "unsigned",
            "void",
            "volatile",
            "while",
            "_Alignas",
            "_Alignof",
            "_Atomic",
            "_Bool",
            "_Complex",
            "_Generic",
            "_Imaginary",
            "_Noreturn",
            "_Pragma",
            "_Static_assert",
            "_Thread_local",
        };
    }
}
