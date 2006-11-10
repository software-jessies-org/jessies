package e.ptextarea;

import java.util.*;

public class PMakefileTextStyler extends PAbstractLanguageStyler {
    // http://www.gnu.org/software/make/manual/html_mono/make.html "Index of Functions, Variables, & Directives":
    private static final String[] KEYWORDS = new String[] {
        "abspath",
        "addprefix",
        "addsuffix",
        "and",
        "basename",
        "bindir",
        "call",
        "define",
        "dir",
        "else",
        "endef",
        "endif",
        "error",
        "eval",
        "exec_prefix",
        "export",
        "filter",
        "filter-out",
        "findstring",
        "firstword",
        "flavor",
        "foreach",
        "if",
        "ifdef",
        "ifeq",
        "ifndef",
        "ifneq",
        "include",
        "info",
        "join",
        "lastword",
        "libexecdir",
        "notdir",
        "or",
        "origin",
        "override",
        "patsubst",
        "prefix",
        "realpath",
        "sbindir",
        "shell",
        "sort",
        "strip",
        "subst",
        "suffix",
        "unexport",
        "value",
        "vpath",
        "warning",
        "wildcard",
        "word",
        "wordlist",
        "words",
    };
    
    public PMakefileTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    protected String getKeywordRegularExpression() {
        return "\\b([a-z_-]+)\\b";
    }
    
    @Override
    protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return isShellComment(line, atIndex);
    }
    
    @Override
    protected boolean supportMultiLineComments() {
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
