package e.ptextarea;

import e.util.*;

/**
 * Implements the core functionality of any real indenter, which is to look at the line in question, split it into indentation and content, work out the new 
 */
public abstract class PSimpleIndenter extends PIndenter {
    public PSimpleIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    public final void fixIndentationOnLine(int lineIndex) {
        String originalIndentation = getCurrentIndentationOfLine(lineIndex);
        String replacementIndentation = getIndentation(lineIndex);
        String originalLine = textArea.getLineText(lineIndex);
        String replacementLine = replacementIndentation + StringUtilities.trimTrailingWhitespace(originalLine.substring(originalIndentation.length()));
        //Log.warn("originalIndentation=@" + originalIndentation + "@; replacementIndentation=@" + replacementIndentation + "@");
        if (replacementLine.equals(originalLine)) {
            return;
        }
        int lineStartOffset = textArea.getLineStartOffset(lineIndex);
        int charsInserted = replacementIndentation.length() - originalIndentation.length();
        int desiredStartOffset = adjustOffsetAfterInsertion(textArea.getSelectionStart(), lineStartOffset, originalIndentation, replacementIndentation);
        int desiredEndOffset = adjustOffsetAfterInsertion(textArea.getSelectionEnd(), lineStartOffset, originalIndentation, replacementIndentation);
        int trimOffset = lineStartOffset + replacementLine.length();
        int charsTrimmed = originalLine.length() - (replacementLine.length() - charsInserted);
        desiredStartOffset = adjustOffsetAfterDeletion(desiredStartOffset, trimOffset, charsTrimmed);
        desiredEndOffset = adjustOffsetAfterDeletion(desiredEndOffset, trimOffset, charsTrimmed);
        textArea.replaceRange(replacementLine, lineStartOffset, lineStartOffset + originalLine.length());
        textArea.select(desiredStartOffset, desiredEndOffset);
    }
    
    /**
     * Returns the indentation which should be used for the given line number.
     * Override this in your subclass to define your indenter's policy.
     * 
     * Copied into a subclass, this example code would implement a copying indenter that simply repeats the indentation of the line above.
     * KDE offers something like this with shift-return.
     * We could usefully do likewise, or offer it as an alternative to the default (non-indenting) indenter for languages that we recognize but don't have a proper indenter for.
     * We'd probably have to change our backspace behavior to stop at a newline, but I've long thought we should do that anyway.
     * 
     * protected String getIndentation(int lineNumber) {
     *     final int previousNonBlankLineNumber = getPreviousNonBlankLineNumber(lineNumber);
     *     return (previousNonBlankLineNumber == -1) ? "" : getCurrentIndentationOfLine(previousNonBlankLineNumber);
     *  }
     */
    protected abstract String getIndentation(int lineNumber);
    
    private static int adjustOffsetAfterInsertion(int offsetToAdjust, int lineStartOffset, String originalIndentation, String replacementIndentation) {
        if (offsetToAdjust < lineStartOffset) {
            return offsetToAdjust;
        } else if (offsetToAdjust > lineStartOffset + originalIndentation.length()) {
            int charsInserted = replacementIndentation.length() - originalIndentation.length();
            return offsetToAdjust + charsInserted;
        } else {
            return lineStartOffset + replacementIndentation.length();
        }
    }
    
    private static int adjustOffsetAfterDeletion(int offsetToAdjust, int offsetOfDeletion, int charsDeleted) {
        if (offsetToAdjust < offsetOfDeletion) {
            return offsetToAdjust;
        } else if (offsetToAdjust > offsetOfDeletion + charsDeleted) {
            return offsetToAdjust - charsDeleted;
        } else {
            return offsetOfDeletion;
        }
    }
}
