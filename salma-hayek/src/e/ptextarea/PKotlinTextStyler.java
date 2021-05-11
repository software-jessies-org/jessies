package e.ptextarea;

import java.util.*;

public class PKotlinTextStyler extends PGenericTextStyler {
    private ArrayList<PSequenceMatcher> matchers = new ArrayList<>();

    public PKotlinTextStyler(PTextArea textArea) {
        super(textArea);
        matchers.add(new PSequenceMatcher.ToEndOfLineComment("//"));
        matchers.add(new PSequenceMatcher.SlashStarComment());
        matchers.add(new PSequenceMatcher.MultiLineString("\"\"\""));
        matchers.add(new PSequenceMatcher.CDoubleQuotes());
        matchers.add(new PSequenceMatcher.CSingleQuotes());
    }

    @Override protected List<PSequenceMatcher> getLanguageSequenceMatchers() {
        return matchers;
    }

    public String[] getKeywords() {
        return new String[] {
            // From https://kotlinlang.org/docs/keyword-reference.html
            // The keywords with punctuation in them (eg "as?", "!in") are not
            // included here, because the keyword highlighter only matches
            // sequences of word characters.
            // Hard keywords:
            "as", "break", "class", "continue", "do", "else", "false", "for",
            "fun", "if", "in", "is", "null", "object", "package",
            "return", "super", "this", "throw", "true", "try", "typealias",
            "typeof", "val", "var", "when", "while",

            // Soft keywords:
            "by", "catch", "constructor", "delegate", "dynamic", "field",
            "file", "finally", "get", "import", "init", "param", "property",
            "receiver", "set", "setparam", "where",

            // Modifier keywords:
            "actual", "abstract", "annotation", "companion", "const",
            "crossinline", "data", "enum", "expect", "external", "final",
            "infix", "inline", "inner", "internal", "lateinit", "noinline",
            "open", "operator", "out", "override", "private", "protected",
            "public", "reified", "sealed", "suspend", "tailrec", "vararg",

            // Special identifiers (minus "field", already defined above):
            "it",

            // Plus types:
            "Boolean", "Byte", "Short", "Int", "Long", "Float", "Double",
            "UByte", "UShort", "UInt", "ULong", "Char", "String",

            // Fixed values:
            "false", "true",
                
            // Useful built-ins (mainly included to avoid the spell checker):
            "println",
        };
    }
}
