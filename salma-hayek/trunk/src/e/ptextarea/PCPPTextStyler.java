package e.ptextarea;

import java.util.*;

/**
 * A PCPPTextStyler knows how to apply syntax highlighting for C++ code.
 * 
 * @author Phil Norman
 */
public class PCPPTextStyler extends PCLikeTextStyler {
    private static final String[] KEYWORDS = new String[] {
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
    
    public PCPPTextStyler(PTextArea textArea, boolean isObjective) {
        super(textArea);
        // "#else" is PREPROCESSOR, but "else" is KEYWORD, so we need to look
        // for preprocessor directives first.
        textArea.addStyleApplicatorFirst(new PreprocessorStyleApplicator(textArea, isObjective));
    }
    
    public boolean supportShellComments() {
        return false;
    }

    public boolean supportDoubleSlashComments() {
        return true;
    }
    
    public void addKeywordsTo(Collection<String> collection) {
        collection.addAll(Arrays.asList(KEYWORDS));
    }
}
