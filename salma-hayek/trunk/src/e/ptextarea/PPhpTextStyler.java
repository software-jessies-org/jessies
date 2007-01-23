package e.ptextarea;

import java.util.*;

public class PPhpTextStyler extends PAbstractLanguageStyler {
    // From http://www.phpbuilder.com/manual/en/reserved.php, sorted by sort(1).
    private static final String[] KEYWORDS = new String[] {
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
    
    public PPhpTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return line.startsWith("//", atIndex);
    }
    
    @Override
    protected boolean supportMultiLineComments() {
        return true;
    }
    
    @Override
    protected String multiLineCommentStart() {
        return "/*";
    }
    
    @Override
    protected String multiLineCommentEnd() {
        return "*/";
    }
    
    @Override
    protected boolean isQuote(char ch) {
        return (ch == '\'' || ch == '\"');
    }
    
    public void addKeywordsTo(Collection<String> collection) {
        collection.addAll(Arrays.asList(KEYWORDS));
    }
}
