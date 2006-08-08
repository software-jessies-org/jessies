package e.ptextarea;

import java.util.*;

public class PBashTextStyler extends PAbstractLanguageStyler {
    private static final String[] KEYWORDS = new String[] {
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
    
    public PBashTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return isShellComment(line, atIndex);
    }
    
    @Override
    protected boolean supportSlashStarComments() {
        return false;
    }
    
    @Override
    protected boolean isQuote(char ch) {
        return (ch == '\'' || ch == '\"' || ch == '`');
    }
    
    public void addKeywordsTo(Collection<String> collection) {
        collection.addAll(Arrays.asList(KEYWORDS));
    }
}
