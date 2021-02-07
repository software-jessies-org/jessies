package e.ptextarea;

import java.util.*;

/**
 * A PCPPTextStyler knows how to apply syntax highlighting for C++ code.
 * 
 * @author Phil Norman
 */
public class PCPPTextStyler extends PGenericTextStyler {
    private ArrayList<PSequenceMatcher> matchers = new ArrayList<>();
    
    public PCPPTextStyler(PTextArea textArea) {
        super(textArea);
        matchers.add(new PSequenceMatcher.ToEndOfLineComment("//"));
        matchers.add(new PSequenceMatcher.SlashStarComment());
        matchers.add(new PSequenceMatcher.CppMultiLineString());
        matchers.add(new PSequenceMatcher.CDoubleQuotes());
        matchers.add(new PSequenceMatcher.CSingleQuotes());
    }
    
    @Override protected List<PSequenceMatcher> getLanguageSequenceMatchers() {
        return matchers;
    }
    
    @Override public void initStyleApplicators() {
        super.initStyleApplicators();
        // "#else" is PREPROCESSOR, but "else" is KEYWORD, so we need to look for preprocessor directives first.
        textArea.addStyleApplicatorFirst(new PreprocessorStyleApplicator(textArea, false));
    }
    
    public String[] getKeywords() {
        return new String[] {
            // https://en.cppreference.com/w/cpp/keyword
            "alignas",
            "alignof",
            "and",
            "and_eq",
            "asm",
            "auto",
            "bitand",
            "bitor",
            "bool",
            "break",
            "case",
            "catch",
            "char",
            "char16_t",
            "char32_t",
            "class",
            "compl",
            "const",
            "constexpr",
            "const_cast",
            "continue",
            "decltype",
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
            "final",
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
            "noexcept",
            "not",
            "not_eq",
            "nullptr",
            "operator",
            "or",
            "or_eq",
            "override",
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
            "static_assert",
            "static_cast",
            "struct",
            "switch",
            "template",
            "this",
            "thread_local",
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
            "xor",
            "xor_eq",
        };
    }
}
