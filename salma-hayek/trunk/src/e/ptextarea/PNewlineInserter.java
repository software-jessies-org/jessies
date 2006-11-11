package e.ptextarea;

import e.util.*;
import java.util.*;
import java.util.regex.*;

public class PNewlineInserter {
    private PTextArea textArea;
    
    public PNewlineInserter(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    private String getLineTextAtOffset(int offset) {
        return textArea.getLineText(textArea.getLineOfOffset(offset));
    }
    
    // PSimpleIndenter has such a method but it's not exposed in the base class.
    private boolean isBlockBegin(CharSequence lineToTheLeft) {
        if (lineToTheLeft.length() == 0) {
            return false;
        }
        char lastChar = lineToTheLeft.charAt(lineToTheLeft.length() - 1);
        if (PBracketUtilities.isOpenBracket(lastChar) == false) {
            return false;
        }
        char closeBracket = PBracketUtilities.getPartnerForBracket(lastChar);
        if (textArea.getIndenter().isElectric(closeBracket) == false) {
            return false;
        }
        return true;
    }
    
    public void insertNewline() {
        textArea.getTextBuffer().getUndoBuffer().startCompoundEdit();
        try {
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
                textArea.replaceSelection("\n");
                textArea.getIndenter().fixIndentationOnLine(startLineIndex);
                textArea.getIndenter().fixIndentation();
            }
        } finally {
            textArea.getTextBuffer().getUndoBuffer().finishCompoundEdit();
        }
    }
    
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
        if (closingBrackets.endsWith("}") == false || textArea.getIndenter().isInNeedOfClosingSemicolon(startLine)) {
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
    
    private void insertMatchingCloseComment() {
        final int position = textArea.getSelectionStart();
        String line = getLineTextAtOffset(position);
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
     * Returns a string corresponding to the spaces and tabs found at the
     * start of the line containing the given offset.
     */
    private String getIndentationOfLineAtOffset(int offset) {
        int lineNumber = textArea.getLineOfOffset(offset);
        return textArea.getIndenter().getCurrentIndentationOfLine(lineNumber);
    }
}
