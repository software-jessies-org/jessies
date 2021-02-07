package e.ptextarea;

import java.util.*;

/**
 * A PJavaTextStyler knows how to apply syntax highlighting for Java code.
 * 
 * @author Phil Norman
 */
public class PJavaTextStyler extends PGenericTextStyler {
    private ArrayList<PSequenceMatcher> matchers = new ArrayList<>();
    
    public PJavaTextStyler(PTextArea textArea) {
        super(textArea);
        matchers.add(new PSequenceMatcher.ToEndOfLineComment("//"));
        matchers.add(new PSequenceMatcher.SlashStarComment());
        matchers.add(new PSequenceMatcher.CDoubleQuotes());
        matchers.add(new PSequenceMatcher.CSingleQuotes());
    }
    
    @Override public void initStyleApplicators() {
        super.initStyleApplicators();
        // "#else" is PREPROCESSOR, but "else" is KEYWORD, so we need to look for preprocessor directives first.
        textArea.addStyleApplicatorFirst(new PreprocessorStyleApplicator(textArea, false));
    }
    
    @Override protected List<PSequenceMatcher> getLanguageSequenceMatchers() {
        return matchers;
    }
    
    public String[] getKeywords() {
        return new String[] {
            // JLS3, section 3.9: "Keywords"
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "if",
            "goto",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "void",
            "volatile",
            "while",
            
            // JLS3, section 3.10.3: "Boolean Literals"
            "true", "false",
            
            // JLS3, section 3.10.7: "The Null Literal"
            "null",
        };
    }
}
