package e.ptextarea;


/**
 * A PCPPTextStyler knows how to apply syntax highlighting for C++ code.
 * 
 * @author Phil Norman
 */

public class PCPPTextStyler extends PCLikeTextStyler {
    private static final String[] CPP_KEYWORDS = new String[] {
        // ISO+IEC+14882-1998 2.11 table 3:
        "asm",
        "auto",
        "bool",
        "break",
        "case",
        "catch",
        "char",
        "class",
        "const",
        "const_cast",
        "continue",
        "default",
        "delete",
        "do",
        "double",
        "dynamic_cast",
        "else",
        "enum",
        "explicit",
        "export",
        "extern",
        "false",
        "float",
        "for",
        "friend",
        "goto",
        "if",
        "inline",
        "int",
        "long",
        "mutable",
        "namespace",
        "new",
        "operator",
        "private",
        "protected",
        "public",
        "register",
        "reinterpret_cast",
        "return",
        "short",
        "signed",
        "sizeof",
        "static",
        "static_cast",
        "struct",
        "switch",
        "template",
        "this",
        "throw",
        "true",
        "try",
        "typedef",
        "typeid",
        "typename",
        "union",
        "unsigned",
        "using",
        "virtual",
        "void",
        "volatile",
        "wchar_t",
        "while",
        // ISO+IEC+14882-1998 2.11 table 4:
        "and",
        "and_eq",
        "bitand",
        "bitor",
        "compl",
        "not",
        "not_eq",
        "or",
        "or_eq",
        "xor",
        "xor_eq",
    };
    
    public PCPPTextStyler(PTextArea textArea) {
        super(textArea);
        addKeywords(CPP_KEYWORDS);
    }
    
    public boolean supportShellComments() {
        return false;
    }
}
