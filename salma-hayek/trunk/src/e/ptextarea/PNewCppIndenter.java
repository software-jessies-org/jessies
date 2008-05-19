package e.ptextarea;

import java.util.regex.*;

/**
 * Basic C++ auto-indenter.
 * 
 * This is still under development, and even when finished won't necessarily
 * guarantee compatibility with the old C++ indenter.
 * 
 * Major goals relative to the old C++ indenter (in no meaningful order):
 * 
 * 1. ability to (optionally) not indent "namespace".
 * 2. ability to cope with wrapped lines.
 * 3. ability to cope with continued lines (for hairy macros).
 * 4. ability to cope with if/else blocks without braces.
 * 5. ability to (optionally) apply a different amount of indentation to
 *    "public:" "protected:" and "private:".
 */
public class PNewCppIndenter extends PSimpleIndenter {
    public PNewCppIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    /**
     * Which characters cause a re-indent of the current line.
     * At the moment, very few.
     */
    @Override
    public boolean isElectric(char c) {
        return (c == '{' || c == '}' || c == '#');
    }
    
    /**
     * Returns the line number of the first line before 'startLineNumber' that
     * contains content relevant for indentation. Preprocessor directives and
     * comments don't count.
     */
    private int getPreviousInterestingLineNumber(int startLineNumber) {
        for (int lineNumber = startLineNumber - 1; lineNumber >= 0; --lineNumber) {
            String activePart = getActivePartOfLine(lineNumber);
            // FIXME: why is this even included by getActivePartOfLine? isn't that a bug? any why doesn't it include, say, PStyle.STRING? surely that's a bug?
            if (activePart.startsWith("#")) {
                continue;
            }
            if (activePart.length() > 0) {
                return lineNumber;
            }
        }
        return -1;
    }
    
    @Override
    protected String calculateNewIndentation(int lineNumber) {
        // Preprocessor directives live in column 0.
        final String currentLine = textArea.getLineText(lineNumber);
        final String activePartOfCurrent = getActivePartOfLine(lineNumber);
        if (activePartOfCurrent.startsWith("#")) {
            return "";
        }
        
        // Search backwards for the first interesting line (called simply
        // "previous line" later, on the assumption that the intervening lines
        // really are uninteresting).
        final int previousLineNumber = getPreviousInterestingLineNumber(lineNumber);
        if (previousLineNumber == -1) {
            return "";
        }
        
        // Get the indentation of the previous line.
        String indentation = getCurrentIndentationOfLine(previousLineNumber);
        
        // Get the previous line, and the non-comment part of the previous line.
        final String previousLine = textArea.getLineText(previousLineNumber);
        final String activePartOfPrevious = getActivePartOfLine(previousLineNumber);
        
        //System.err.println("'" + activePartOfPrevious + "'; indentation '" + indentation + "'");
        
        // Handle simple cases.
        if (activePartOfPrevious.trim().endsWith("{") && activePartOfPrevious.matches("^namespace\\b.*") == false) {
            indentation = increaseIndentation(indentation);
        } else if (activePartOfCurrent.trim().endsWith("}")) {
            indentation = decreaseIndentation(indentation);
        }
        
        return indentation + extraBlockCommentArtForLine(lineNumber);
    }
    
    /**
     * Whether PNewlineInserter should also add a trailing ';' if it inserts a
     * closing '}' for the given line.
     */
    @Override
    public boolean isInNeedOfClosingSemicolon(String line) {
        return line.matches(".*\\b(class|enum|struct|union)\\b.*");
    }
}
