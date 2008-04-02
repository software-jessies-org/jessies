package e.ptextarea;

import e.util.*;
import java.util.*;

/**
 * Implements the core functionality of any real indenter, which is to look at the line in question, split it into indentation and content, work out the new 
 */
public abstract class PSimpleIndenter extends PIndenter {
    public PSimpleIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    public final void fixIndentationOnLine(int lineIndex) {
        String originalIndentation = getCurrentIndentationOfLine(lineIndex);
        String replacementIndentation = calculateNewIndentation(lineIndex);
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
     */
    protected abstract String calculateNewIndentation(int lineNumber);
    
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
    
    protected final int getPreviousNonBlankLineNumber(int startLineNumber) {
        for (int lineNumber = startLineNumber - 1; lineNumber >= 0; --lineNumber) {
            if (textArea.getLineText(lineNumber).trim().length() != 0) {
                return lineNumber;
            }
        }
        return -1;
    }
    
    protected final String getActivePartOfLine(int lineIndex) {
        StringBuilder activePartOfLine = new StringBuilder();
        for (PLineSegment segment : textArea.getLineSegments(lineIndex)) {
            PStyle style = segment.getStyle();
            if (style == PStyle.NORMAL || style == PStyle.KEYWORD || style == PStyle.PREPROCESSOR) {
                activePartOfLine.append(segment.getCharSequence());
            }
        }
        return activePartOfLine.toString().trim();
    }
    
    // Recognize block comments, and help out with the ASCII art.
    protected final String extraBlockCommentArtForLine(int lineIndex) {
        if (lineIndex <= 0) {
            return "";
        }
        List<PLineSegment> previousLineSegments = textArea.getLineSegments(lineIndex - 1);
        if (previousLineSegments.size() == 0) {
            return "";
        }
        // Extract the previous line's comment text.
        String previousLineCommentText = "";
        for (PLineSegment segment : previousLineSegments) {
            if (segment.getStyle() == PStyle.COMMENT) {
                previousLineCommentText += segment.getCharSequence();
            }
        }
        previousLineCommentText = previousLineCommentText.trim();
        
        // Extract this line's text. I think we can safely infer whether or not it's comment (see below).
        String currentLineText = textArea.getLineText(lineIndex).trim();
        
        // NewlineInserter treats /** and /* the same way, so we should too.
        if (previousLineCommentText.startsWith("/*") || previousLineCommentText.startsWith("*")) {
            // We're either part-way through, or on the line after, a block comment.
            if (previousLineCommentText.endsWith("*/")) {
                // We're on the line after, so we must leave the current line's indentation as it is.
                return "";
            } else if (currentLineText.startsWith("**")) {
                // We're on a "boxed" block comment, so just add the leading space to line up.
                return " ";
            } else if (currentLineText.endsWith("*/")) {
                // We're on the last line, so add a leading " " to line the "*/" on this line up with the "*" above.
                return " ";
            } else {
                // We're part-way through a JavaDoc-style block comment, so add a leading " * ".
                return " * ";
            }
        }
        return "";
    }
}
