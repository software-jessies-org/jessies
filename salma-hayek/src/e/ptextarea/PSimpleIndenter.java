package e.ptextarea;

import e.util.*;
import java.util.*;
import java.util.regex.*;

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
        if (replacementIndentation == null) {
            return;
        }
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
     * You may return null to indicate that the indentation of the line is already correct.
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
    // TODO: nothing calls this function. It should probably move to the Java indenter, and be actually
    // called by the java-specific indentation/newline-insertion stuff.
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
    
    ///////////////////////////////////////////////////////////////////////////////////////////
    //
    // HERE BE DRAGONS
    //
    // The following is a copy/paste of what used to be in PNewlineInserter.
    // The logic of how to react when the user hits Return properly belongs
    // in the individual language indenters, not in some pseudo-generic class.
    // The setup with the PNewlineInserter was broken for several reasons:
    //  1: It had its own copies of certain bits of logic.
    //  2: There were several bizarre functions which had to be exposed on the
    //     indenter interface to make things sort of work.
    //  3: It had numerous bugs, particularly in Go, where it'd add semicolons
    //     to 'var ()' blocks, and insert random close-curly-brackets when
    //     editing inline struct data declarations.
    //
    // All of the following code is just a copy/paste, and so doesn't directly
    // make life better. The plan is to now start picking it apart, and replacing
    // it with proper, language-specific code to deal with this stuff properly.
    // The following code will eventually all go away.
    //
    ///////////////////////////////////////////////////////////////////////////////////////////
    
    // Inserts a newline (and does whatever indentation fixing).
    public void insertNewlineImpl(boolean fixIndentation) {
        final int startPosition = textArea.getSelectionStart();
        CharSequence chars = textArea.getTextBuffer();
        
        int startLineIndex = textArea.getLineOfOffset(startPosition);
        int startLineStartOffset = textArea.getLineStartOffset(startLineIndex);
        CharSequence lineToTheLeft = chars.subSequence(startLineStartOffset, startPosition);
        
        if (isBlockBegin(lineToTheLeft) && insertMatchingBrackets()) {
            return;
        }
        
        if (isUnclosedComment(chars, startPosition, lineToTheLeft)) {
            insertMatchingCloseComment();
        } else {
            textArea.replaceSelection("\n" + getCurrentIndentationOfLine(startLineIndex));
            if (fixIndentation) {
                fixIndentationOnLine(startLineIndex);
                fixIndentation();
            }
        }
    }
    
    // PCFamilyIndenter has such a method but it's not exposed in the base class.
    private boolean isBlockBegin(CharSequence lineToTheLeft) {
        if (lineToTheLeft.length() == 0) {
            return false;
        }
        char lastChar = lineToTheLeft.charAt(lineToTheLeft.length() - 1);
        if (PBracketUtilities.isOpenBracket(lastChar) == false) {
            return false;
        }
        return isElectric(PBracketUtilities.getPartnerForBracket(lastChar));
    }
    
    private String getLineTextAtOffset(int offset) {
        return textArea.getLineText(textArea.getLineOfOffset(offset));
    }
    
    // FIXME: assumes C-family multi-line comments.
    private boolean isUnclosedComment(CharSequence entireDocument, int insertionPosition, CharSequence lineToTheLeft) {
        if (Pattern.matches("[ \t]*/\\*{1,2}", lineToTheLeft)) {
            // We're on a line that starts a block comment, but is it unclosed?
            int nextOpenComment = StringUtilities.indexOf(entireDocument, "/*", insertionPosition);
            int nextCloseComment = StringUtilities.indexOf(entireDocument, "*/", insertionPosition);
            if (nextCloseComment == -1) {
                // If there are no close comments after this point, this one we're looking at must be unclosed.
                return true;
            }
            if (nextOpenComment != -1 && nextOpenComment < nextCloseComment) {
                // If there's an open comment after this point, and no intervening close comment, the one we're looking at must be unclosed.
                return true;
            }
        }
        return false;
    }
    
    // TODO: Doesn't belong here.
    private static String getCommonEnding(String left, String right) {
        StringBuilder ending = new StringBuilder();
        for (int i = 0; i < left.length() && i < right.length(); ++i) {
            char leftChar = left.charAt(left.length() - 1 - i);
            char rightChar = right.charAt(right.length() - 1 - i);
            if (leftChar != rightChar) {
                break;
            }
            ending.append(leftChar);
        }
        return ending.toString();
    }
    
    // FIXME: this is a hack for the benefit of PNewlineInserter, which does its own indentation fixing when inserting matching brackets, rather than deferring to the indenter. (Why is that the case?)
    public boolean isInNeedOfClosingSemicolon(String line) {
        return false;
    }
    
    private boolean insertMatchingBrackets() {
        final int start = textArea.getSelectionStart();
        final int end = textArea.getSelectionEnd();
        int endLineIndex = textArea.getLineOfOffset(end);
        int suffixPosition = textArea.getLineEndOffsetBeforeTerminator(endLineIndex);
        String beforeInsertion = textArea.getTextBuffer().subSequence(0, start).toString();
        String afterInsertion = textArea.getTextBuffer().subSequence(suffixPosition, textArea.getTextBuffer().length()).toString();
        String unmatchedOpenBrackets = getUnmatchedBrackets(beforeInsertion);
        String unmatchedCloseBrackets = getUnmatchedBrackets(afterInsertion);
        String reflectedCloseBrackets = PBracketUtilities.reflectBrackets(unmatchedCloseBrackets);
        if (unmatchedOpenBrackets.startsWith(reflectedCloseBrackets) == false) {
            return false;
        }
        String closingBrackets = PBracketUtilities.reflectBrackets(unmatchedOpenBrackets.substring(reflectedCloseBrackets.length()));
        if (closingBrackets.length() == 0) {
            return false;
        }
        String startLine = getLineTextAtOffset(start);
        if (closingBrackets.endsWith("}") == false || isInNeedOfClosingSemicolon(startLine)) {
            // TODO: "closingBrackets" is a bad name now it can have a semicolon on the end!
            closingBrackets = closingBrackets + ";";
        }
        String candidateBlockContents = textArea.getTextBuffer().subSequence(end, suffixPosition).toString();
        String commonEnding = getCommonEnding(candidateBlockContents, closingBrackets);
        String whitespace = getIndentationOfLineAtOffset(start);
        // TODO: The newline inserter has no business thinking it knows how to increase the indent.
        String prefix = "\n" + whitespace + textArea.getIndentationString();
        String suffix = "\n" + whitespace + closingBrackets;
        final int newCaretPosition = start + prefix.length();
        textArea.replaceSelection(prefix);
        // suffixPosition is invalidated by replaceSelection.
        // But we can't swap the calls because replaceRange clears the selection.
        int selectionSize = end - start;
        suffixPosition -= selectionSize;
        suffixPosition += prefix.length();
        textArea.replaceRange(suffix, suffixPosition - commonEnding.length(), suffixPosition);
        textArea.select(newCaretPosition, newCaretPosition);
        return true;
    }
    
    // FIXME: assumes C-family multi-line comments.
    private void insertMatchingCloseComment() {
        final int position = textArea.getSelectionStart();
        String whitespace = getIndentationOfLineAtOffset(position);
        String prefix = "\n" + whitespace + " * ";
        String suffix = "\n" + whitespace + " */";
        textArea.replaceSelection(prefix + suffix);
        final int newOffset = position + prefix.length();
        textArea.select(newOffset, newOffset);
    }
    
    private static String getUnmatchedBrackets(final String initialText) {
        String text = initialText.replaceAll("\\\\.", "_"); // Remove escaped characters.
        text = text.replaceAll("'.'", "_"); // Remove character literals.
        text = text.replaceAll("\"([^\\n]*?)\"", "_"); // Remove string literals.
        text = text.replaceAll("/\\*(?s).*?\\*/", "_"); // Remove C comments.
        text = text.replaceAll("//[^\\n]*", "_"); // Remove C++ comments.
        StringBuilder unmatchedBrackets = new StringBuilder();
        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (PBracketUtilities.isOpenBracket(ch) && ch != '<') {
                unmatchedBrackets.append(ch);
            } else if (PBracketUtilities.isCloseBracket(ch) && ch != '>') {
                char openBracket = PBracketUtilities.getPartnerForBracket(ch);
                int lastCharIndex = unmatchedBrackets.length() - 1;
                if (lastCharIndex >= 0 && unmatchedBrackets.charAt(lastCharIndex) == openBracket) {
                    unmatchedBrackets.deleteCharAt(lastCharIndex);
                } else {
                    unmatchedBrackets.append(ch);
                }
            }
        }
        return unmatchedBrackets.toString();
    }
    
    /**
    Returns a string corresponding to the spaces and tabs found at the
    start of the line containing the given offset.
    */
    private String getIndentationOfLineAtOffset(int offset) {
        int lineNumber = textArea.getLineOfOffset(offset);
        return getCurrentIndentationOfLine(lineNumber);
    }
}
