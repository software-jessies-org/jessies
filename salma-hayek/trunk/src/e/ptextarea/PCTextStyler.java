package e.ptextarea;

import java.util.*;

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
            "int",
            "long",
            "register",
            "return",
            "short",
            "signed",
            "static",
            "struct",
            "switch",
            "typedef",
            "union",
            "unsigned",
            "void",
            "volatile",
            "while"
        };
    }
}
