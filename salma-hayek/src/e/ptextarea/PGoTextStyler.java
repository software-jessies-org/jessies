package e.ptextarea;

import java.util.*;

public class PGoTextStyler extends PGenericTextStyler {
    private ArrayList<PSequenceMatcher> matchers = new ArrayList<>();
    
    public PGoTextStyler(PTextArea textArea) {
        super(textArea);
        matchers.add(new PSequenceMatcher.ToEndOfLineComment("//"));
        matchers.add(new PSequenceMatcher.SlashStarComment());
        matchers.add(new PSequenceMatcher.CDoubleQuotes());
        matchers.add(new PSequenceMatcher.CSingleQuotes());
        matchers.add(new PSequenceMatcher.MultiLineString("`"));
    }
    
    @Override protected List<PSequenceMatcher> getLanguageSequenceMatchers() {
        return matchers;
    }
    
    public String[] getKeywords() {
        return new String[] {
            // From https://golang.org/ref/spec#Keywords
            "break", "default", "func", "interface", "select",
            "case", "defer", "go", "map", "struct",
            "chan", "else", "goto", "package", "switch",
            "const", "fallthrough", "if", "range", "type",
            "continue", "for", "import", "return", "var",

            // Plus types:
            "bool", "byte", "rune",
            "uint8", "uint16", "uint32", "uint64",
            "int8", "int16", "int32", "int64",
            "float32", "float64", "complex64", "complex128",
            "uint", "int", "uintptr",
            "string", "error",

            // Fixed values:
            "false", "true", "nil",

            // Builtins:
            "delete",
        };
    }
}
