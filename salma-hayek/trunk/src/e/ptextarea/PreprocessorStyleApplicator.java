package e.ptextarea;

import java.util.regex.*;

/**
 * Styles pre-processor directives.
 * 
 * It doesn't support %: because I've never seen that in real life.
 * It doesn't support recognizing #if 0/#endif and treating it as a multi-line
 * comment, which vim(1) does.
 */
public class PreprocessorStyleApplicator extends RegularExpressionStyleApplicator {
    private static final String PATTERN = "\\s*([#]\\s*(define|elif|else|endif|error|ifdef|ifndef|include|import|line|pragma|undef|warn|warning))\\b";
    
    private boolean isObjectiveC;
    
    public PreprocessorStyleApplicator(PTextArea textArea, boolean isObjectiveC) {
        super(textArea, PATTERN, PStyle.PREPROCESSOR);
        this.isObjectiveC = isObjectiveC;
    }
    
    @Override
    public boolean isAcceptableMatch(CharSequence line, Matcher matcher) {
        // FIXME:
        // A preprocessor directive doesn't have to start in column 0, but it
        // must be the first thing on a line. It's a bit more complicated than
        // that, though, because comments don't count. So our
        // PSameStyleCharSequence isn't suitable. Maybe we need something that
        // takes an EnumSet<PStyle> of the styles you do (or don't?) want.
        
        if (isObjectiveC) {
            // #include is legal in Objective C++, but you want #import instead.
            return (matcher.group(2).equals("include") == false);
        } else {
            // #import is not legal in other languages.
            return (matcher.group(2).equals("import") == false);
        }
    }
}
