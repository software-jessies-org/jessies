package e.ptextarea;

import java.util.*;

public class PLuaTextStyler extends PGenericTextStyler {
    private ArrayList<PSequenceMatcher> matchers = new ArrayList<>();
    
    public PLuaTextStyler(PTextArea textArea) {
        super(textArea);
        matchers.add(new PSequenceMatcher.StartEndStyle(PStyle.COMMENT, "--[[", "--]]"));
        matchers.add(new PSequenceMatcher.ToEndOfLineComment("--"));
        matchers.add(new PSequenceMatcher.StartEndStyle(PStyle.STRING, "[[", "]]"));
        matchers.add(new PSequenceMatcher.PythonSingleQuotes());
        matchers.add(new PSequenceMatcher.CDoubleQuotes());
    }
    
    @Override protected List<PSequenceMatcher> getLanguageSequenceMatchers() {
        return matchers;
    }
    
    public String[] getKeywords() {
        return new String[] {
            "and", "break", "do", "else", "elseif",
            "end", "false", "for", "function",  "if",
            "in", "local", "nil", "not", "or",
            "repeat", "return", "then", "true", "until",
            "while",
        };
    }
}
