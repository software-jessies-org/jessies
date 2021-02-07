package e.ptextarea;

import java.util.*;

public class PBashTextStyler extends PGenericTextStyler {
    private ArrayList<PSequenceMatcher> matchers = new ArrayList<>();
    
    public PBashTextStyler(PTextArea textArea) {
        super(textArea);
        matchers.add(new PSequenceMatcher.ToEndOfLineComment("#"));
        matchers.add(new PSequenceMatcher.MultiLineString("'"));
        matchers.add(new PSequenceMatcher.MultiLineString("\""));
        matchers.add(new PSequenceMatcher.MultiLineString("`"));  // Not really a string as such, but never mind.
        matchers.add(new PSequenceMatcher.BashHereDoc());
    }
    
    @Override protected List<PSequenceMatcher> getLanguageSequenceMatchers() {
        return matchers;
    }
    
    @Override protected String getKeywordRegularExpression() {
        // The usual \w+ is insufficient, because Bash doesn't use as much punctuation as most languages.
        // make-source and command-list, for example, are each interpreted as a single word.
        // GtkSourceView and Vim both get both examples wrong, considering "source" and "command" to be keywords.
        // We accept anything that isn't a Bash meta-character, which seems hard to fool in practice.
        return "\\b(([^ \t<>;&|])+)\\b";
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
