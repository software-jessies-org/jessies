package e.ptextarea;

/**
 * Applies colors to assembly language source.
 */
public class PAssemblerTextStyler extends PAbstractLanguageStyler {
    public PAssemblerTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override public void initStyleApplicators() {
        super.initStyleApplicators();
        textArea.addStyleApplicatorFirst(new PreprocessorStyleApplicator(textArea, false));
    }
    
    @Override protected String getKeywordRegularExpression() {
        // We don't include the leading '.' in the keyword so that the list of keywords is suitable for passing to the spelling checker.
        // This has the unfortunate side-effect of not coloring the leading '.' as if it were part of the directive.
        // Marking colored keywords as spelling mistakes seems like the worse evil, though.
        // A fix for this, should assembly language become important enough to us, would be to specify which group is used for coloring and which is used for checking the keyword set, rather than having both hard-coded to group 1 as now.
        return "(?:\\s|)\\.([a-z][a-z0-9_]+)\\b";
    }
    
    @Override protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        if (line.startsWith("//", atIndex)) {
            // C++ comments are allowed.
            return true;
        } else if (line.startsWith("#", atIndex)) {
            // For compatibility with old assemblers, gas(1) also accepts # as comment-to-EOL most of the time.
            // ARM also uses "#" for integer literals ("mov r0, #1", say).
            // Insisting on the "#" appearing at the start of the line or having a space after it seems like a likely heuristic.
            if (atIndex == 0 || line.length() == atIndex + 1 || line.charAt(atIndex + 1) == ' ') {
                return true;
            }
        }
        // FIXME: for some assembly languages (SPARC, say) "!" begins a comment-to-EOL, but for others (ARM, say) it's used in instructions such as "stmfd sp!, {r0-r8}".
        return false;
    }
    
    @Override protected boolean supportMultiLineComments() {
        return true;
    }
    
    public String[] getKeywords() {
        return new String[] {
            // gas() 2.17 manual: http://sourceware.org/binutils/docs-2.17/as/Pseudo-Ops.html#Pseudo-Ops
            "abort",
            "align",
            "altmacro",
            "ascii",
            "asciz",
            "balign",
            "balignl",
            "balignw",
            "byte",
            "comm",
            "cfi_startproc",
            "cfi_endproc",
            "data",
            "def",
            "desc",
            "dim",
            "double",
            "eject",
            "else",
            "elseif",
            "endef",
            "endfunc",
            "endif",
            "equ",
            "equiv",
            "eqv",
            "err",
            "error",
            "exitm",
            "extern",
            "fail",
            "file",
            "fill",
            "float",
            "func",
            "global",
            "globl",
            "hidden",
            "hword",
            "ident",
            "if",
            "incbin",
            "include",
            "int",
            "internal",
            "irp",
            "irpc",
            "lcomm",
            "lflags",
            "line",
            "linkonce",
            "list",
            "ln",
            "loc",
            "long",
            "macro",
            "mri",
            "noaltmacro",
            "nolist",
            "octa",
            "org",
            "p2align",
            "p2alignl",
            "p2alignw",
            "popsection",
            "previous",
            "print",
            "protected",
            "psize",
            "purgem",
            "pushsection",
            "quad",
            "rept",
            "sbttl",
            "scl",
            "section",
            "set",
            "short",
            "single",
            "size",
            "skip",
            "sleb128",
            "space",
            "stabd",
            "stabn",
            "stabs",
            "string",
            "struct",
            "subsection",
            "symver",
            "tag",
            "text",
            "title",
            "type",
            "uleb128",
            "val",
            "version",
            "vtable_entry",
            "vtable_inherit",
            "warning",
            "weak",
            "weakref",
            "word",
            // Extras.
            "arch",
            "code16",
        };
    }
}
