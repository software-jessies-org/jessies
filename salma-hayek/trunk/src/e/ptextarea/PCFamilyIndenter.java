package e.ptextarea;

import e.util.*;
import java.util.*;

/**
 * Implements indentation for members of the C family, parameterized to cater for their differences.
 */
public abstract class PCFamilyIndenter extends PSimpleIndenter {
    public PCFamilyIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    public boolean isElectric(char c) {
        if (c == '#' && shouldMoveHashToColumnZero()) {
            return true;
        }
        if (c == ':' && shouldMoveLabels()) {
            return true;
        }
        if (c == '<' && shouldMoveOperatorOut()) {
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
    private boolean isDefinitive(String activePartOfLine) {
        return isBlockBegin(activePartOfLine) || isBlockEnd(activePartOfLine) || isLabel(activePartOfLine);
    }
    
    public int getPreviousDefinitiveLineNumber(int startLineNumber) {
        for (int lineIndex = startLineNumber - 1; lineIndex >= 0; --lineIndex) {
            String line = getActivePartOfLine(lineIndex);
            if (isDefinitive(line)) {
                return lineIndex;
            }
        }
        return -1;
    }
    
    @Override
    public String calculateNewIndentation(int lineIndex) {
        String activePartOfLine = getActivePartOfLine(lineIndex);
        
        if (shouldMoveHashToColumnZero() && activePartOfLine.startsWith("#")) {
            return "";
        }
        
        // A special case for C++'s operator<<.
        if (shouldMoveOperatorOut() && activePartOfLine.startsWith("<<")) {
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
        int previousDefinitive = getPreviousDefinitiveLineNumber(lineIndex);
        if (previousDefinitive != -1) {
            indentation = getCurrentIndentationOfLine(previousDefinitive);
            
            String activePartOfPrevious = getActivePartOfLine(previousDefinitive);
            if (isBlockBegin(activePartOfPrevious) || isLabel(activePartOfPrevious)) {
                indentation = increaseIndentation(indentation);
            }
        }
        
        if (isBlockEnd(activePartOfLine) || isLabel(activePartOfLine)) {
            indentation = decreaseIndentation(indentation);
        }
        
        indentation += extraBlockCommentArtForLine(lineIndex);
        
        return indentation;
    }
    
    protected abstract boolean isLabel(String activePartOfLine);
    protected abstract boolean shouldMoveHashToColumnZero();
    protected abstract boolean shouldMoveLabels();
    protected abstract boolean shouldMoveOperatorOut();
}
