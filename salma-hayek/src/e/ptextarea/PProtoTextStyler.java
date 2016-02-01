package e.ptextarea;

/**
 * A text styler for Google protocol buffers (http://code.google.com/p/protobuf).
 */
public class PProtoTextStyler extends PAbstractLanguageStyler {
    public PProtoTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return line.startsWith("//", atIndex);
    }
    
    @Override protected boolean supportMultiLineComments() {
        return true;
    }
    
    @Override protected boolean isQuote(char ch) {
        return (ch == '\"');
    }
    
    public String[] getKeywords() {
        return new String[] {
            // http://code.google.com/p/protobuf/source/browse/trunk/editors/proto.vim
            "syntax", "import", "option",
            "package", "message", "group",
            "optional", "required", "repeated",
            "default",
            "extend", "extensions", "to", "max",
            "service", "rpc", "returns",
            
            "int32", "int64", "uint32", "uint64", "sint32", "sint64",
            "fixed32", "fixed64", "sfixed32", "sfixed64",
            "float", "double", "bool", "string", "bytes",
            "enum",
            "true", "false",
        };
    }
}
