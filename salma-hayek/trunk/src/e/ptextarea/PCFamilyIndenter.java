package e.ptextarea;

import e.util.*;

/**
 * Implements indentation for members of the C family, parameterized to cater for their differences.
 */
public abstract class PCFamilyIndenter extends PSimpleIndenter {
    public PCFamilyIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    private void debug(String message) {
        if (false) {
            System.err.println(message);
        }
    }
    
    @Override public boolean isElectric(char c) {
        FileType fileType = textArea.getFileType();
        if ((c == '#' || c == '<') && (fileType == FileType.C_PLUS_PLUS)) {
            // C++ wants special handling of operator<< and pre-processor directives.
            return true;
        }
        if (c == ':' && (fileType == FileType.C_PLUS_PLUS || fileType == FileType.JAVA)) {
            // C++ and Java want special handling of goto and switch labels, and C++ access specifiers.
            return true;
        }
        if (PBracketUtilities.isCloseBracket(c)) {
            return true;
        }
        return false;
    }
    
    public boolean isBlockBegin(String activePartOfLine) {
        if (activePartOfLine.length() == 0) {
            return false;
        }
        char lastChar = activePartOfLine.charAt(activePartOfLine.length() - 1);
        if ((lastChar == '<') && (activePartOfLine.length() >= 2)) {
            // We must not consider left-shifts to be block starts.
            return (activePartOfLine.charAt(activePartOfLine.length() - 2) != '<');
        } else {
            return PBracketUtilities.isOpenBracket(lastChar);
        }
    }
    
    public boolean isBlockEnd(String activePartOfLine) {
        if (activePartOfLine.length() == 0) {
            return false;
        }
        char firstChar = activePartOfLine.charAt(0);
        if ((firstChar == '>') && (activePartOfLine.length() >= 2)) {
            // We must not consider right-shifts to be block ends.
            return (activePartOfLine.charAt(1) != '>');
        } else {
            return PBracketUtilities.isCloseBracket(firstChar);
        }
    }
    
    public boolean isSwitchLabel(String activePartOfLine) {
        return activePartOfLine.matches("(case\\b.*|default\\s*):.*");
    }
    
    /**
     * Lines that start with a closing brace or end with a opening brace or a colon tell us
     * definitively what the indentation for the next line should be.
     * Going back this far keeps us tidy in the face of various multi-line comment styles,
     * multi-line C++ output operator expressions and C++ preprocessor commands.
     * 
     * It does assume a style that never wraps, but is otherwise surprisingly solid for Java.
     * It doesn't work as well for C++, where there are many conflicting styles.
     * 
     * FIXME: sometimes this is an under-estimate. For example:
     *     { // This counts as "definitive"
     *         object.method(some_long_expression, some_other_long_expression,
     *                       // This line should start here, because it's a continuation of the previous line.
     *         // But this is where we assume it starts.
     * 
     * FIXME: other times, this is an over-estimate. For example:
     * void function(int i,
     *               double d) {
     *     // This line should start here.
     *                   // But this is where we assume it starts.
     * 
     * FIXME: yet other times, many styles wouldn't indent at all. For example:
     * namespace {
     * class C; // This line should often start here.
     *     // But this is where we assume it starts.
     * }
     * (Here, both styles are common, sadly.)
     * 
     * FIXME: yet other times, we should pay more attention to the "definitive" line's indentation. For example:
     * class C {
     *   public: // This is a half-indent.
     *     // This line should start here.
     *       // But this is where we assume it starts.
     * }
     * (Here, non-indented access specifiers and half-indented ones are both common. It might be best to just ignore them completely.)
     */
    private DefinitiveLine getPreviousDefinitiveLine(int startLineNumber) {
        for (int lineIndex = startLineNumber - 1; lineIndex >= 0; --lineIndex) {
            String activePartOfLine = getActivePartOfLine(lineIndex);
            if (isBlockBegin(activePartOfLine)) {
                if (activePartOfLine.endsWith(") {")) {
                    // Find the matching opening parenthesis: *that* line is the definitive one.
                    final String wholeLine = textArea.getLineText(lineIndex);
                    final int closingParenthesisOffset = textArea.getLineStartOffset(lineIndex) + wholeLine.lastIndexOf(") {");
                    debug("need to find matching opening paren for closing paren at offset" + closingParenthesisOffset + " on line " + lineIndex + "...");
                    int openingParenthesisOffset = PBracketUtilities.findMatchingBracketInSameStyle(textArea, closingParenthesisOffset);
                    if (openingParenthesisOffset == -1) {
                        // We're confused; keep searching.
                        continue;
                    } else {
                        final int openingParenthesisLineIndex = textArea.getLineOfOffset(openingParenthesisOffset);
                        debug("translated offset " + openingParenthesisOffset + " to line number " + openingParenthesisLineIndex + "!");
                        return new DefinitiveLine(openingParenthesisLineIndex, false);
                    }
                }
                return new DefinitiveLine(lineIndex, false);
            } else if (isBlockEnd(activePartOfLine)) {
                // I'm not aware of any case where a closing brace isn't truly definitive.
                return new DefinitiveLine(lineIndex, true);
            } else if (isLabel(activePartOfLine)) {
                // Ignore these, as per this method's doc comment?
                //return lineIndex;
                continue;
            }
        }
        return null;
    }
    
    private static class DefinitiveLine {
        int lineIndex;
        boolean closing;
        DefinitiveLine(int lineIndex, boolean closing) {
            this.lineIndex = lineIndex;
            this.closing = closing;
        }
    }
    
    @Override public String calculateNewIndentation(int lineIndex) {
        FileType fileType = textArea.getFileType();
        
        String activePartOfLine = getActivePartOfLine(lineIndex);
        
        // A special case for C++'s preprocessor.
        if (fileType == FileType.C_PLUS_PLUS && activePartOfLine.startsWith("#")) {
            return "";
        }
        
        // A special case for C++'s operator<<.
        if (fileType == FileType.C_PLUS_PLUS && activePartOfLine.startsWith("<<")) {
            if (lineIndex == 0) {
                return "";
            }
            String previousLine = textArea.getLineText(lineIndex - 1);
            int previousOperatorOutIndex = previousLine.indexOf("<<");
            if (previousOperatorOutIndex != -1) {
                return StringUtilities.nCopies(previousOperatorOutIndex, ' ');
            }
        }
        
        String indentation = "";
        DefinitiveLine previousDefinitive = getPreviousDefinitiveLine(lineIndex);
        if (previousDefinitive != null) {
            indentation = getCurrentIndentationOfLine(previousDefinitive.lineIndex);
            debug("indentation1='"+indentation+"'");
            
            if (!previousDefinitive.closing) {
                indentation = increaseIndentation(indentation);
                debug("indentation2='"+indentation+"'");
            }
        }
        
        if (isBlockEnd(activePartOfLine) || isLabel(activePartOfLine)) {
            indentation = decreaseIndentation(indentation);
            debug("indentation3='"+indentation+"'");
        }
        
        indentation += extraBlockCommentArtForLine(lineIndex);
        debug("indentation4='"+indentation+"'");
        
        return indentation;
    }
    
    protected abstract boolean isLabel(String activePartOfLine);
}
