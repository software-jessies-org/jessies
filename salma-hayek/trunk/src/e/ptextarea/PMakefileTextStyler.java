package e.ptextarea;

import java.util.*;

public class PMakefileTextStyler extends PAbstractLanguageStyler {
    // http://www.gnu.org/software/make/manual/html_mono/make.html "Index of Functions, Variables, & Directives":
    private static final String[] KEYWORDS = new String[] {
        "addprefix",
        "addsuffix",
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
        "foreach",
        "if",
        "ifdef",
        "ifeq",
        "ifndef",
        "ifneq",
        "include",
        "join",
        "libexecdir",
        "notidr",
        "origin",
        "override",
        "patsubst",
        "prefix",
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
    protected boolean supportShellComments() {
        return true;
    }
    
    @Override
    protected boolean supportDoubleSlashComments() {
        return false;
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
