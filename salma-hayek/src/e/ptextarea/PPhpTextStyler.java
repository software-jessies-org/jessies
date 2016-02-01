package e.ptextarea;

public class PPhpTextStyler extends PAbstractLanguageStyler {
    public PPhpTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override protected String getKeywordRegularExpression() {
        // PHP variables begin with a $, and for any keyword k, $k is a valid identifier.
        return "\\b(?<!\\$)([A-Za-z_]+)\\b";
    }
    
    @Override protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return line.startsWith("//", atIndex) || isShellComment(line, atIndex);
    }
    
    @Override protected boolean supportMultiLineComments() {
        return true;
    }
    
    @Override protected boolean isQuote(char ch) {
        return (ch == '\'' || ch == '\"' || ch == '`');
    }
    
    // From http://www.phpbuilder.com/manual/en/reserved.php, sorted by sort(1).
    public String[] getKeywords() {
        return new String[] {
            "abstract",
            "and",
            "array",
            "as",
            "break",
            "case",
            "catch",
            "cfunction",
            "class",
            "__CLASS__",
            "clone",
            "const",
            "continue",
            "declare",
            "default",
            "die",
            "do",
            "echo",
            "else",
            "elseif",
            "empty",
            "enddeclare",
            "endfor",
            "endforeach",
            "endif",
            "endswitch",
            "endwhile",
            "eval",
            "exception",
            "exit",
            "extends",
            "__FILE__",
            "final",
            "for",
            "foreach",
            "function",
            "__FUNCTION__",
            "global",
            "if",
            "implements",
            "include",
            "include_once",
            "interface",
            "isset",
            "__LINE__",
            "list",
            "__METHOD__",
            "new",
            "old_function",
            "or",
            "php_user_filter",
            "print",
            "private",
            "protected",
            "public",
            "require",
            "require_once",
            "return",
            "static",
            "switch",
            "throw",
            "try",
            "unset",
            "use",
            "var",
            "while",
            "xor"
        };
    }
}
