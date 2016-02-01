package e.ptextarea;

public class PBashTextStyler extends PAbstractLanguageStyler {
    public PBashTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override protected String getKeywordRegularExpression() {
        // The usual \w+ is insufficient, because Bash doesn't use as much punctuation as most languages.
        // make-source and command-list, for example, are each interpreted as a single word.
        // GtkSourceView and Vim both get both examples wrong, considering "source" and "command" to be keywords.
        // We accept anything that isn't a Bash meta-character, which seems hard to fool in practice.
        return "\\b(([^ \t<>;&|])+)\\b";
    }
    
    @Override protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return isShellComment(line, atIndex);
    }
    
    @Override protected boolean supportMultiLineComments() {
        return false;
    }
    
    @Override protected boolean isQuote(char ch) {
        return (ch == '\'' || ch == '\"' || ch == '`');
    }
    
    public String[] getKeywords() {
        return new String[] {
            // "Compound Commands", bash man page.
            "case",
            "do",
            "done",
            "elif",
            "else",
            "esac",
            "for",
            "fi",
            "function",
            "if",
            "in",
            "select",
            "then",
            "until",
            "while",
            // "SHELL BUILTIN COMMANDS", bash man page.
            "source",
            "alias",
            "bg",
            "bind",
            "break",
            "builtin",
            "cd",
            "caller",
            "command",
            "compgen",
            "complete",
            "continue",
            "declare",
            "typeset",
            "dirs",
            "disown",
            "echo",
            "enable",
            "eval",
            "exec",
            "exit",
            "export",
            "fc",
            "fg",
            "getopts",
            "hash",
            "help",
            "history",
            "jobs",
            "kill",
            "let",
            "local",
            "logout",
            "popd",
            "printf",
            "pushd",
            "pwd",
            "read",
            "readonly",
            "return",
            "set",
            "shift",
            "shopt",
            "suspend",
            "test",
            "times",
            "trap",
            "type",
            "ulimit",
            "umask",
            "unalias",
            "unset",
            "wait"
        };
    }
}
