package e.ptextarea;

/**
 * A PRustTextStyler knows how to apply syntax highlighting for rust code.

 * @author Phil Norman
 */
public class PRustTextStyler extends PAbstractLanguageStyler {
    public PRustTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override public void initStyleApplicators() {
        super.initStyleApplicators();
        // "#else" is PREPROCESSOR, but "else" is KEYWORD, so we need to look for preprocessor directives first.
        textArea.addStyleApplicatorFirst(new PreprocessorStyleApplicator(textArea, false));
    }
    
    @Override protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return line.startsWith("//", atIndex);
    }
    
    @Override protected boolean supportMultiLineComments() {
        return true;
    }
    
    public String[] getKeywords() {
        // See https://doc.rust-lang.org/reference/keywords.html
        return new String[] {
            "as",
            "async",
            "await",
            "break",
            "const",
            "continue",
            "crate",
            "dyn",
            "else",
            "enum",
            "extern",
            "false",
            "fn",
            "for",
            "if",
            "impl",
            "in",
            "let",
            "loop",
            "match",
            "mod",
            "move",
            "mut",
            "pub",
            "ref",
            "return",
            "self",
            "Self",
            "static",
            "struct",
            "super",
            "trait",
            "true",
            "type",
            "unsafe",
            "use",
            "where",
            "while",
            
            // Types.
            "bool", "char",
            "f32", "f64",
            "i8", "i16", "i32", "i64", "i128", "isize",
            "u8", "u16", "u32", "u64", "u128", "usize",
        };
    }
}
