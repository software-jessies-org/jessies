package e.ptextarea;

import java.util.*;

public class PMakefileTextStyler extends PAbstractLanguageStyler {
    // http://www.gnu.org/software/make/manual/make.html#Quick-Reference
    private static final String[] DIRECTIVES = new String[] {
        "define",
        "endef",
        "ifdef",
        "ifndef",
        "ifeq",
        "ifneq",
        "else",
        "endif",
        "include",
        "sinclude",
        "override",
        "export",
        "unexport",
        "vpath",
    };
    private static final String[] FUNCTIONS = new String[] {
        "subst",
        "patsubst",
        "strip",
        "findstring",
        "filter",
        "filter-out",
        "sort",
        "word",
        "wordlist",
        "words",
        "firstword",
        "lastword",
        "dir",
        "notdir",
        "suffix",
        "basename",
        "addsuffix",
        "addprefix",
        "join",
        "wildcard",
        "realpath",
        "abspath",
        "if",
        "or",
        "and",
        "foreach",
        "call",
        "value",
        "eval",
        "origin",
        "flavor",
        "shell",
        "error",
        "warning",
        "info",
    };
    
    public PMakefileTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return isShellComment(line, atIndex);
    }
    
    @Override protected boolean supportMultiLineComments() {
        return false;
    }
    
    @Override protected boolean isQuote(char ch) {
        // FIXME: strictly, you can use ' instead of " after ifeq or ifneq, and it might be better to treat ' as a quote in rules' commands, but naively accepting ' leads to false matches with $(warning) text.
        return (ch == '\"');
    }
    
    @Override public void initStyleApplicators() {
        // Make functions are -- except in special cases like when they're arguments to "call" -- always preceded by "$(" or "${".
        // I didn't realize until testing this (and I've checked the source to see I'm not imagining things) that you can't have whitespace between the "$(" and the keyword.
        textArea.addStyleApplicator(new KeywordStyleApplicator(textArea, new HashSet<String>(Arrays.asList(FUNCTIONS)), "\\b(?<=\\$[({])([a-z_-]+)\\b"));
        // Make directives are just plain old words.
        textArea.addStyleApplicator(new KeywordStyleApplicator(textArea, new HashSet<String>(Arrays.asList(DIRECTIVES)), "\\b(\\w+)\\b"));
        // There are also various automatic variables.
        textArea.addStyleApplicator(new RegularExpressionStyleApplicator(textArea, "(\\$[@%<?^+*]|\\$\\([@%<?^+*][DF]\\))", PStyle.KEYWORD));
        // And finally, special built-in target names.
        textArea.addStyleApplicator(new RegularExpressionStyleApplicator(textArea, "^(\\.(PHONY|SUFFIXES|DEFAULT|PRECIOUS|INTERMEDIATE|SECONDARY|SECONDEXPANSION|DELETE_ON_ERROR|IGNORE|LOW_RESOLUTION_TIME|SILENT|EXPORT_ALL_VARIABLES|NOTPARALLEL))", PStyle.KEYWORD));
    }
    
    public String[] getKeywords() {
        ArrayList<String> result = new ArrayList<String>();
        result.addAll(Arrays.asList(DIRECTIVES));
        result.addAll(Arrays.asList(FUNCTIONS));
        return result.toArray(new String[result.size()]);
    }
}
