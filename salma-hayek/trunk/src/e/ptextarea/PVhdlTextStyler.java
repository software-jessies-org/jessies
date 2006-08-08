package e.ptextarea;

import java.util.*;

public class PVhdlTextStyler extends PAbstractLanguageStyler {
    // http://www.iis.ee.ethz.ch/~zimmi/download/vhdl02_syntax.html "Reserved Words":
    private static final String[] KEYWORDS = new String[] {
        "abs",
        "access",
        "after",
        "alias",
        "all",
        "and",
        "architecture",
        "array",
        "assert",
        "attribute",
        "begin",
        "block",
        "body",
        "buffer",
        "bus",
        "case",
        "component",
        "configuration",
        "constant",
        "disconnect",
        "downto",
        "else",
        "elsif",
        "end",
        "entity",
        "exit",
        "file",
        "for",
        "function",
        "generate",
        "generic",
        "group",
        "guarded",
        "if",
        "impure",
        "in",
        "inertial",
        "inout",
        "is",
        "label",
        "library",
        "linkage",
        "literal",
        "loop",
        "map",
        "mod",
        "nand",
        "new",
        "next",
        "nor",
        "not",
        "null",
        "of",
        "on",
        "open",
        "or",
        "others",
        "out",
        "package",
        "port",
        "postponed",
        "procedure",
        "process",
        "protected",
        "pure",
        "range",
        "record",
        "register",
        "reject",
        "rem",
        "report",
        "return",
        "rol",
        "ror",
        "select",
        "severity",
        "shared",
        "signal",
        "sla",
        "sll",
        "sra",
        "srl",
        "subtype",
        "then",
        "to",
        "transport",
        "type",
        "unaffected",
        "units",
        "until",
        "use",
        "variable",
        "wait",
        "when",
        "while",
        "with",
        "xnor",
        "xor",
    };
    
    public PVhdlTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return line.startsWith("--", atIndex);
    }
    
    @Override
    protected boolean supportSlashStarComments() {
        return false;
    }
    
    @Override
    protected boolean isQuote(char ch) {
        return (ch == '\"');
    }
    
    public void addKeywordsTo(Collection<String> collection) {
        collection.addAll(Arrays.asList(KEYWORDS));
    }
}
