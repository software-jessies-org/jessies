package e.ptextarea;

import java.util.*;

public class PPythonTextStyler extends PGenericTextStyler {
    private ArrayList<PSequenceMatcher> matchers = new ArrayList<>();
    
    public PPythonTextStyler(PTextArea textArea) {
        super(textArea);
        matchers.add(new PSequenceMatcher.ToEndOfLineComment("#"));
        // Multiline strings *must* be added before plain strings, or the multiline bit won't be noticed.
        matchers.add(new PSequenceMatcher.MultiLineString("'''"));
        matchers.add(new PSequenceMatcher.MultiLineString("\"\"\""));
        matchers.add(new PSequenceMatcher.CDoubleQuotes());
        matchers.add(new PSequenceMatcher.PythonSingleQuotes());
    }
    
    @Override protected List<PSequenceMatcher> getLanguageSequenceMatchers() {
        return matchers;
    }
    
    public String[] getKeywords() {
        return new String[] {
            // python -c 'import keyword ; print keyword.kwlist' | tr "' []" '"\n  '
            "and",
            "as",
            "assert",
            "break",
            "class",
            "continue",
            "def",
            "del",
            "elif",
            "else",
            "except",
            "exec",
            "finally",
            "for",
            "from",
            "global",
            "if",
            "import",
            "in",
            "is",
            "lambda",
            "not",
            "or",
            "pass",
            "print",
            "raise",
            "return",
            "try",
            "while",
            "with",
            "yield",
        };
    }
}
