package e.ptextarea;

import java.util.regex.*;

/**
 * Basic Python auto-indenter.
 */
public class PPythonIndenter extends PSimpleIndenter {
    public PPythonIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    public boolean isElectric(char c) {
        return ("abcdefghijklmnopqrstuvwxyz:".indexOf(c) != -1);
    }
    
    @Override
    protected String calculateNewIndentation(int lineNumber) {
        // Python indenter, based on "python.vim".
        // Most of the comments are recognizably similar to the Vim comments.
        // Original Vim indenter author was David Bustos <bustos@caltech.edu>.
        
        // FIXME: handle \-continued lines.
        
        // FIXME: handle lines within multi-line strings.
        
        // Search backwards for the first non-empty line (called simply "previous line" later).
        // If this is the first non-empty line, use no indentation.
        final int previousNonBlankLineNumber = getPreviousNonBlankLineNumber(lineNumber);
        if (previousNonBlankLineNumber == -1) {
            return "";
        }
        
        // FIXME: handle unfinished parenthesized expressions (see also the end of this method).
        
        // Get the indentation of the previous line.
        final String indentation = getCurrentIndentationOfLine(previousNonBlankLineNumber);
        
        // Get the previous line and remove any trailing comment.
        // FIXME: use styler information.
        String previousLine = textArea.getLineText(previousNonBlankLineNumber);
        int commentIndex = previousLine.indexOf("#");
        if (commentIndex != -1) {
            previousLine = previousLine.substring(0, commentIndex);
        }
        final String currentLine = textArea.getLineText(lineNumber);
        
        // If the previous line ended with a colon, indent this line.
        if (previousLine.trim().endsWith(":")) {
            return increaseIndentation(indentation);
        }
        
        // If the previous line was a stop-execution statement...
        if (previousLine.matches("^\\s*(break|continue|raise|return|pass)\\b.*$")) {
            // If the user has already reduced the indentation, trust them.
            if (getCurrentIndentationOfLine(lineNumber).length() <= getCurrentIndentationOfLine(previousNonBlankLineNumber).length() - textArea.getIndentationString().length()) {
                return getCurrentIndentationOfLine(lineNumber);
            }
            // They haven't, so do it for them.
            return decreaseIndentation(indentation);
        }
        
        // If the current line begins with a keyword that lines up with "try"...
        if (currentLine.matches("^\\s*(except|finally)\\b.*$")) {
            // Find the matching "try".
            for (int tryLineNumber = lineNumber - 1; tryLineNumber >= 0; --tryLineNumber) {
                String tryLine = textArea.getLineText(tryLineNumber);
                if (tryLine.matches("^\\s*(try|except)\\b.*$")) {
                    String tryIndentation = indentationOf(tryLine);
                    if (tryIndentation.length() >= getCurrentIndentationOfLine(lineNumber).length()) {
                        // Indentation is already less than this.
                        return getCurrentIndentationOfLine(lineNumber);
                    }
                    // Line up with this try/except.
                    return tryIndentation;
                }
            }
            // No matching try/except.
            return getCurrentIndentationOfLine(lineNumber);
        }
        
        // If the current line begins with a header keyword, reduce the indentation.
        if (currentLine.matches("^\\s*(elif|else)\\b.*$")) {
            // Unless the previous line was a one-liner...
            if (previousLine.matches("^\\s*(for|if|try)\\b.*$")) {
                return indentation;
            }
            // ...or the user has already reduced the indentation.
            if (getCurrentIndentationOfLine(lineNumber).length() <= getCurrentIndentationOfLine(previousNonBlankLineNumber).length() - textArea.getIndentationString().length()) {
                return getCurrentIndentationOfLine(lineNumber);
            }
            return decreaseIndentation(indentation);
        }
        
        // FIXME: Vim has a final special case here for the conclusion of a multi-line parenthesized expression.
        
        return getCurrentIndentationOfLine(lineNumber);
    }
}
