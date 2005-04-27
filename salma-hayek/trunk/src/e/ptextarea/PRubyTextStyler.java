package e.ptextarea;

public class PRubyTextStyler extends PCLikeTextStyler {
    // http://www.rubycentral.com/book/language.html table 18.3 "Reserved Words":
    private static final String[] RUBY_KEYWORDS = new String[] {
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
        addKeywords(RUBY_KEYWORDS);
    }
    
    public boolean supportShellComments() {
        return true;
    }

    public boolean supportDoubleSlashComments() {
        return false;
    }
}
