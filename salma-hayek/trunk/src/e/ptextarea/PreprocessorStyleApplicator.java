package e.ptextarea;

import java.util.regex.*;

/**
 * Styles pre-processor directives.
 * 
 * It doesn't check that we're the first non-whitespace on a line.
 * It doesn't support %: because I've never seen that in real life.
 * It doesn't support recognizing #if 0/#endif and treating it as a multi-line
 * comment, which vim(1) does.
 */
public class PreprocessorStyleApplicator extends RegularExpressionStyleApplicator {
    private static final String PATTERN = "\\s*[#]\\s*(define|elif|else|endif|error|ifdef|ifndef|include|import|line|pragma|undef|warn|warning)\\b";
    
    private boolean isObjectiveC;
    
    public PreprocessorStyleApplicator(PTextArea textArea, boolean isObjectiveC) {
        super(textArea, PATTERN, PStyle.PREPROCESSOR);
        this.isObjectiveC = isObjectiveC;
    }
    
    @Override
    public boolean isGoodMatch(Matcher matcher) {
        if (isObjectiveC) {
            // #include is legal in Objective C++, but you want #import instead.
            return (matcher.group(1).equals("include") == false);
        } else {
            // #import is not legal in other languages.
            return (matcher.group(1).equals("import") == false);
        }
    }
}
