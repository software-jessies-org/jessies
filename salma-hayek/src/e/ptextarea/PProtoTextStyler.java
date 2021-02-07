package e.ptextarea;

import java.util.*;

/**
 * A text styler for Google protocol buffers (http://code.google.com/p/protobuf).
 */
public class PProtoTextStyler extends PGenericTextStyler {
    private ArrayList<PSequenceMatcher> matchers = new ArrayList<>();
    
    public PProtoTextStyler(PTextArea textArea) {
        super(textArea);
        matchers.add(new PSequenceMatcher.ToEndOfLineComment("//"));
        matchers.add(new PSequenceMatcher.SlashStarComment());
        matchers.add(new PSequenceMatcher.CDoubleQuotes());
        matchers.add(new PSequenceMatcher.PythonSingleQuotes());
    }
    
    @Override protected List<PSequenceMatcher> getLanguageSequenceMatchers() {
        return matchers;
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
