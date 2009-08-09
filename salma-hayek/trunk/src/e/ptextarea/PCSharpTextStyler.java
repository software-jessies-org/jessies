package e.ptextarea;

import java.util.*;

public class PCSharpTextStyler extends PAbstractLanguageStyler {
    public PCSharpTextStyler(PTextArea textArea) {
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
            // http://en.wikibooks.org/wiki/C_Sharp_Programming/Keywords/ref
            "abstract",
            "as",
            "base",
            "bool",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "checked",
            "class",
            "const",
            "continue",
            "decimal",
            "default",
            "delegate",
            "do",
            "double",
            "else",
            "enum",
            "event",
            "explicit",
            "extern",
            "false",
            "finally",
            "fixed",
            "float",
            "for",
            "foreach",
            "goto",
            "if",
            "implicit",
            "in",
            "int",
            "interface",
            "internal",
            "is",
            "lock",
            "long",
            "namespace",
            "new",
            "null",
            "object",
            "operator",
            "out",
            "override",
            "params",
            "private",
            "protected",
            "public",
            "readonly",
            "ref",
            "return",
            "sbyte",
            "sealed",
            "short",
            "sizeof",
            "stackalloc",
            "static",
            "string",
            "struct",
            "switch",
            "this",
            "throw",
            "true",
            "try",
            "typeof",
            "uint",
            "ulong",
            "unchecked",
            "unsafe",
            "ushort",
            "using",
            "virtual",
            "void",
            "volatile",
            "while",
            
            // These ones are context-sensitive, I think.
            "add",
            "alias",
            "get",
            "global",
            "partial",
            "remove",
            "set",
            "value",
            "where",
            "yield",
        };
    }
}
