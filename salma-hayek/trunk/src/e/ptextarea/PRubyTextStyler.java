package e.ptextarea;

import java.util.*;

public class PRubyTextStyler extends PAbstractLanguageStyler {
    public PRubyTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override public void initStyleApplicators() {
        super.initStyleApplicators();
        // An approximate attempt to recognize Ruby regular expression literals.
        // Canonical list of valid options taken from regx_options() in "parse.y" of Ruby 1.8.5; all documentation I've seen is incorrect.
        // FIXME: this doesn't work for regular expressions containing quotes, because PAbstractLanguageStyler will already have styled them before the regular expression style applicators are run.
        // FIXME: this doesn't work for regular expressions occurring on the lhs of a =~ expression.
        // FIXME: this doesn't work for %r literals (or any other of Ruby's % literals).
        textArea.addStyleApplicator(new RegularExpressionStyleApplicator(textArea, "[=~(]\\s*(/[^/]*/[ixmonesu])", PStyle.STRING));
    }
    
    @Override protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return isShellComment(line, atIndex);
    }
    
    @Override protected boolean supportMultiLineComments() {
        return true;
    }
    
    @Override protected String multiLineCommentStart() {
        // FIXME: only true at the beginning of a line.
        return "=begin";
    }
    
    @Override protected String multiLineCommentEnd() {
        // FIXME: only true at the beginning of a line.
        return "=end";
    }
    
    @Override protected boolean isQuote(char ch) {
        return (ch == '\'' || ch == '\"' || ch == '`');
    }
    
    // http://www.rubycentral.com/book/language.html table 18.3 "Reserved Words":
    public String[] getKeywords() {
        return new String[] {
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
    }
}
