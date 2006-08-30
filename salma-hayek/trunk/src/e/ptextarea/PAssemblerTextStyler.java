package e.ptextarea;

import java.util.*;

/**
 * Applies colors to assembly language source.
 */
public class PAssemblerTextStyler extends PAbstractLanguageStyler {
    private static final String[] KEYWORDS = new String[] {
        // gas() 2.17 manual: http://sourceware.org/binutils/docs-2.17/as/Pseudo-Ops.html#Pseudo-Ops
        ".abort",
        ".align",
        ".altmacro",
        ".ascii",
        ".asciz",
        ".balign",
        ".balignl",
        ".balignw",
        ".byte",
        ".comm",
        ".cfi_startproc",
        ".cfi_endproc",
        ".data",
        ".def",
        ".desc",
        ".dim",
        ".double",
        ".eject",
        ".else",
        ".elseif",
        ".endef",
        ".endfunc",
        ".endif",
        ".equ",
        ".equiv",
        ".eqv",
        ".err",
        ".error",
        ".exitm",
        ".extern",
        ".fail",
        ".file",
        ".fill",
        ".float",
        ".func",
        ".global",
        ".hidden",
        ".hword",
        ".ident",
        ".if",
        ".incbin",
        ".include",
        ".int",
        ".internal",
        ".irp",
        ".irpc",
        ".lcomm",
        ".lflags",
        ".line",
        ".linkonce",
        ".list",
        ".ln",
        ".loc",
        ".long",
        ".macro",
        ".mri",
        ".noaltmacro",
        ".nolist",
        ".octa",
        ".org",
        ".p2align",
        ".p2alignl",
        ".p2alignw",
        ".popsection",
        ".previous",
        ".print",
        ".protected",
        ".psize",
        ".purgem",
        ".pushsection",
        ".quad",
        ".rept",
        ".sbttl",
        ".scl",
        ".section",
        ".set",
        ".short",
        ".single",
        ".size",
        ".skip",
        ".sleb128",
        ".space",
        ".stabd",
        ".stabn",
        ".stabs",
        ".string",
        ".struct",
        ".subsection",
        ".symver",
        ".tag",
        ".text",
        ".title",
        ".type",
        ".uleb128",
        ".val",
        ".version",
        ".vtable_entry",
        ".vtable_inherit",
        ".warning",
        ".weak",
        ".weakref",
        ".word",
        // Extras.
        ".arch",
        ".code16",
    };
    
    public PAssemblerTextStyler(PTextArea textArea) {
        super(textArea);
        textArea.addStyleApplicatorFirst(new PreprocessorStyleApplicator(textArea, false));
    }
    
    @Override
    protected String getKeywordRegularExpression() {
        return "(?:\\s|)(\\.[a-z][a-z0-9_]+)\\b";
    }
    
    @Override
    protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return line.startsWith("//", atIndex);
    }
    
    @Override
    protected boolean supportSlashStarComments() {
        return true;
    }
    
    public void addKeywordsTo(Collection<String> collection) {
        collection.addAll(Arrays.asList(KEYWORDS));
    }
}
