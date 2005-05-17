package e.ptextarea;

import java.util.*;

public class PNewlineInserter {
    private PTextArea textArea;
    
    public PNewlineInserter(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    private String getLineTextAtOffset(int offset) {
        return textArea.getLineText(textArea.getLineOfOffset(offset));
    }
    
    public void insertNewline() {
        textArea.getTextBuffer().getUndoBuffer().startCompoundEdit();
        try {
            final int startPosition = textArea.getSelectionStart();
            if (startPosition > 0) {
                char lastChar = textArea.getTextBuffer().charAt(startPosition - 1);
                if (PBracketUtilities.isOpenBracket(lastChar)) {
                    if (insertMatchingBrackets()) {
                        return;
                    }
                }
            }
            
            int startLineIndex = textArea.getLineOfOffset(startPosition);
            int startLineStartOffset = textArea.getLineStartOffset(startLineIndex);
            String lineBeforeInsertionPoint = getLineTextAtOffset(startPosition).substring(0, startPosition - startLineStartOffset);
            if (lineBeforeInsertionPoint.matches("[ \t]*/\\*{1,2}")) {
                insertMatchingCloseComment();
            } else {
                textArea.replaceSelection("\n");
                textArea.getIndenter().fixIndentationAt(startPosition);
                textArea.getIndenter().fixIndentation();
            }
        } finally {
            textArea.getTextBuffer().getUndoBuffer().finishCompoundEdit();
        }
    }
    
    // TODO: Doesn't belong here.
    public static String getCommonEnding(String left, String right) {
        StringBuffer ending = new StringBuffer();
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
    
    public void insertMatchingCloseComment() {
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
        StringBuffer unmatchedBrackets = new StringBuffer();
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
