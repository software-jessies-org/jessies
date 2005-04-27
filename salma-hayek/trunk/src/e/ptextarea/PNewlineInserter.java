package e.ptextarea;

public class PNewlineInserter {
    private PTextArea textArea;
    
    public PNewlineInserter(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    private String getLineTextAtOffset(int offset) {
        return textArea.getLineText(textArea.getLineOfOffset(offset));
    }
    
    private boolean shouldInsertMatchingBracket(char lastChar) {
        if (PBracketUtilities.isOpenBracket(lastChar)) {
            char openBracket = lastChar;
            if (hasUnbalancedBrackets(textArea.getText(), openBracket)) {
                return true;
            }
        }
        return false;
    }
    
    public void insertNewline() {
        textArea.getTextBuffer().getUndoBuffer().startCompoundEdit();
        try {
            // FIXME - selection
            final int position = textArea.getSelectionStart();
            
            // Should we try to insert matching brace pairs?
            String line = getLineTextAtOffset(position);
            if (position > 0) {
                char lastChar = textArea.getTextBuffer().charAt(position - 1);
                if (shouldInsertMatchingBracket(lastChar)) {
                    insertMatchingBracket(lastChar);
                    return;
                }
            }
            if (line.endsWith("/*") || line.endsWith("/**")) {
                insertMatchingCloseComment();
            } else {
                textArea.replaceSelection("\n");
                textArea.autoIndent();
            }
        } finally {
            textArea.getTextBuffer().getUndoBuffer().finishCompoundEdit();
        }
    }
    
    public void insertMatchingBracket(char openBracket) {
        final char closeBracket = PBracketUtilities.getPartnerForBracket(openBracket);
        final int start = textArea.getSelectionStart();
        final int end = textArea.getSelectionEnd();
        int endLineIndex = textArea.getLineOfOffset(end);
        int suffixPosition = textArea.getLineEndOffsetBeforeTerminator(endLineIndex);
        String startLine = getLineTextAtOffset(start);
        String whitespace = getIndentationOfLineAtOffset(start);
        String prefix = "\n" + whitespace + textArea.getIndentationString();
        String suffix = "\n" + whitespace + closeBracket;
        if (textArea.getIndenter().isInNeedOfClosingSemicolon(startLine)) {
            suffix += ";";
        }
        final int newCaretPosition = start + prefix.length();
        textArea.replaceSelection(prefix);
        // suffixPosition is invalidated by replaceSelection.
        // But we can't swap the calls because replaceRange clears the selection.
        int selectionSize = end - start;
        suffixPosition -= selectionSize;
        suffixPosition += prefix.length();
        textArea.replaceRange(suffix, suffixPosition, suffixPosition);
        textArea.select(newCaretPosition, newCaretPosition);
    }
    
    public void insertMatchingCloseComment() {
        // FIXME - selection?
        final int position = textArea.getSelectionStart();
        String line = getLineTextAtOffset(position);
        String whitespace = getIndentationOfLineAtOffset(position);
        String prefix = "\n" + whitespace + " * ";
        String suffix = "\n" + whitespace + " */";
        textArea.insert(prefix + suffix);
        final int newOffset = position + prefix.length();
        textArea.select(newOffset, newOffset);
    }
    
    public boolean hasUnbalancedBrackets(final String text, char openBracket) {
        return (calculateBracketNesting(text, openBracket) != 0);
    }
    
    /**
     * Returns how many more opening braces there are than closing
     * braces in the given String. Returns 0 if there are equal
     * numbers of opening and closing braces; a negative number if
     * there are more closing braces than opening braces.
     */
    public int calculateBracketNesting(final String initialText, char openBracket) {
        final char closeBracket = PBracketUtilities.getPartnerForBracket(openBracket);
        String text = initialText.replaceAll("\\\\.", "_"); // Remove escaped characters.
        text = text.replaceAll("'.'", "_"); // Remove character literals.
        text = text.replaceAll("\"([^\\n]*?)\"", "_"); // Remove string literals.
        text = text.replaceAll("/\\*(?s).*?\\*/", "_"); // Remove C comments.
        text = text.replaceAll("//[^\\n]*", "_"); // Remove C++ comments.
        int nestingDepth = 0;
        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (ch == openBracket) {
                ++nestingDepth;
            } else if (ch == closeBracket) {
                --nestingDepth;
            }
        }
        return nestingDepth;
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
