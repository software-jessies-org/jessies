package e.ptextarea;

import e.util.*;
import java.util.regex.*;

public abstract class PIndenter {
    private static final Pattern INDENTATION_PATTERN = Pattern.compile("(^[ \\t]*(?:\\*(?: |$))?).*$");
    
    protected PTextArea textArea;
    
    public PIndenter(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    protected final String increaseIndentation(String original) {
        return original + textArea.getIndentationString();
    }
    
    protected final String decreaseIndentation(String original) {
        String delta = textArea.getIndentationString();
        if (original.endsWith(delta)) {
            return original.substring(0, original.length() - delta.length());
        }
        return original;
    }
    
    /**
     * Returns true if c is an 'electric' character, which is emacs terminology
     * for a character that may cause the indentation to be modified when you
     * type it. Typically, this signifies the end of a block.
     * Note the "may": this saves us correcting the indentation on every
     * character insertion.  That's an optimization but also prevents us being
     * unusable when the indenter doesn't support the indentation style
     * in use.
     */
    public abstract boolean isElectric(char c);
    
    /**
     * Returns a copy of just the leading part of the given line.
     * Usually that just contains whitespace but the asterisks at the start
     * of the body lines of a doc-comment are also considered as indentation
     * because they end in (a single character of) whitespace.
     */
    public final String getCurrentIndentationOfLine(int lineNumber) {
        String line = textArea.getLineText(lineNumber);
        Matcher matcher = INDENTATION_PATTERN.matcher(line);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            throw new IllegalArgumentException("line number " + lineNumber + " \"" + line + "\" has impossible indentation");
        }
    }
    
    /** Returns the indentation which should be used for the given line number. */
    public String getIndentation(int lineNumber) {
        final int previousNonBlankLineNumber = getPreviousNonBlankLineNumber(lineNumber);
        return (previousNonBlankLineNumber == -1) ? "" : getCurrentIndentationOfLine(previousNonBlankLineNumber);
    }
    
    public boolean isInNeedOfClosingSemicolon(String line) {
        return false;
    }
    
    protected final int getPreviousNonBlankLineNumber(int startLineNumber) {
        for (int lineNumber = startLineNumber - 1; lineNumber >= 0; lineNumber--) {
            if (textArea.getLineText(lineNumber).trim().length() != 0) {
                return lineNumber;
            }
        }
        return -1;
    }
    
    /** Corrects the indentation of the lines with the selection. Returns true if the contents of the current line were changed. */
    public final void fixIndentation() {
        fixIndentationBetween(textArea.getSelectionStart(), textArea.getSelectionEnd());        
    }
    
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
    
    public final void fixIndentationBetween(final int start, final int end) {
        final int startLine = textArea.getLineOfOffset(start);
        // I've thought about (and experimented with) the +-1 issue here.
        final int finishLine = textArea.getLineOfOffset(end);
        for (int line = startLine; line <= finishLine; ++line) {
            fixIndentationAt(textArea.getLineStartOffset(line));
        }
    }

    public void fixIndentationAt(int position) {
        int lineIndex = textArea.getLineOfOffset(position);
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
}
