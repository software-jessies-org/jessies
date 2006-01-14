package e.ptextarea;

import java.util.*;

public class PRubyTextStyler extends PCLikeTextStyler {
    // http://www.rubycentral.com/book/language.html table 18.3 "Reserved Words":
    private static final String[] KEYWORDS = new String[] {
        "__FILE__",
        "__LINE__",
        "BEGIN",
        "END",
        "alias",
        "and",
        "begin",
        "break",
        "case",
        "class",
        "def",
        "defined?",
        "do",
        "else",
        "elsif",
        "end",
        "ensure",
        "false",
        "for",
        "if",
        "in",
        "module",
        "next",
        "nil",
        "not",
        "or",
        "redo",
        "rescue",
        "retry",
        "return",
        "self",
        "super",
        "then",
        "true",
        "undef",
        "unless",
        "until",
        "when",
        "while",
        "yield",
    };
    
    public PRubyTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    public boolean supportShellComments() {
        return true;
    }

    @Override
    public boolean supportDoubleSlashComments() {
        return false;
    }
    
    public void addKeywordsTo(Collection<String> collection) {
        collection.addAll(Arrays.asList(KEYWORDS));
    }
}
