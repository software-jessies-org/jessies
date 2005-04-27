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
            // FIXME - selection
            final int position = textArea.getSelectionStart();
            
            // Should we try to insert matching brackets?
            String line = getLineTextAtOffset(position);
            if (position > 0) {
                char lastChar = textArea.getTextBuffer().charAt(position - 1);
                if (PBracketUtilities.isOpenBracket(lastChar)) {
                    if (insertMatchingBrackets()) {
                        return;
                    }
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
    
    private boolean insertMatchingBrackets() {
        final int start = textArea.getSelectionStart();
        final int end = textArea.getSelectionEnd();
        int endLineIndex = textArea.getLineOfOffset(end);
        int suffixPosition = textArea.getLineEndOffsetBeforeTerminator(endLineIndex);
        StringBuffer textToSearchForBrackets = new StringBuffer(textArea.getText());
        textToSearchForBrackets.delete(start, suffixPosition);
        String closingBrackets = getMissingClosingBrackets(textToSearchForBrackets.toString());
        if (closingBrackets.length() == 0) {
            return false;
        }
        String startLine = getLineTextAtOffset(start);
        String whitespace = getIndentationOfLineAtOffset(start);
        String prefix = "\n" + whitespace + textArea.getIndentationString();
        String suffix = "\n" + whitespace + closingBrackets;
        if (closingBrackets.endsWith("}") == false || textArea.getIndenter().isInNeedOfClosingSemicolon(startLine)) {
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
        return true;
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
        
    private static String getMissingClosingBrackets(final String initialText) {
        String text = initialText.replaceAll("\\\\.", "_"); // Remove escaped characters.
        text = text.replaceAll("'.'", "_"); // Remove character literals.
        text = text.replaceAll("\"([^\\n]*?)\"", "_"); // Remove string literals.
        text = text.replaceAll("/\\*(?s).*?\\*/", "_"); // Remove C comments.
        text = text.replaceAll("//[^\\n]*", "_"); // Remove C++ comments.
        Stack openBrackets = new Stack();
        StringBuffer missingClosingBrackets = new StringBuffer();
        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (PBracketUtilities.isOpenBracket(ch) && ch != '<') {
                openBrackets.push(new Character(ch));
            } else if (PBracketUtilities.isCloseBracket(ch) && ch != '>') {
                while (openBrackets.empty() == false) {
                    char openBracket = ((Character) openBrackets.pop()).charValue();
                    char closeBracket = PBracketUtilities.getPartnerForBracket(openBracket);
                    if (closeBracket == ch) {
                        break;
                    }
                    missingClosingBrackets.append(closeBracket);
                }
            }
        }
        while (openBrackets.empty() == false) {
            char openBracket = ((Character) openBrackets.pop()).charValue();
            char closeBracket = PBracketUtilities.getPartnerForBracket(openBracket);
            missingClosingBrackets.append(closeBracket);
        }
        return missingClosingBrackets.toString();
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
