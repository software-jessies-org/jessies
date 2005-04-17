package e.ptextarea;

public class PNewlineInserter {
    private PTextArea textArea;
    
    public PNewlineInserter(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    private String getLineTextAtOffset(int offset) {
        return textArea.getLineText(textArea.getLineOfOffset(offset));
    }
    
    public void insertNewline() {
        // FIXME: start CompoundEdit
        
        // FIXME - selection
        final int position = textArea.getSelectionStart();
        
        // Should we try to insert matching brace pairs?
        String line = getLineTextAtOffset(position);
        if (textArea.getIndenter().isElectric('}') && position > 0 && textArea.getPTextBuffer().charAt(position - 1) == '{' && hasUnbalancedBraces(textArea.getText())) {
            insertMatchingBrace();
        } else if (line.endsWith("/*") || line.endsWith("/**")) {
            insertMatchingCloseComment();
        } else {
            textArea.replaceSelection("\n");
            textArea.autoIndent();
        }
        
        // FIXME: end CompoundEdit
    }
    
    public void insertMatchingBrace() {
        // FIXME - selection
        final int position = textArea.getSelectionStart();
        String line = getLineTextAtOffset(position);
        String whitespace = getIndentationOfLineAtOffset(position);
        String prefix = "\n" + whitespace + textArea.getIndentationString();
        String suffix = "\n" + whitespace + "}";
        if (textArea.getIndenter().isInNeedOfClosingSemicolon(line)) {
            suffix += ";";
        }
        final int newCaretPosition = position + prefix.length();
        textArea.replaceSelection(prefix + suffix);
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
    
    public boolean hasUnbalancedBraces(final String text) {
        return (calculateBraceNesting(text) != 0);
    }
    
    /**
     * Returns how many more opening braces there are than closing
     * braces in the given String. Returns 0 if there are equal
     * numbers of opening and closing braces; a negative number if
     * there are more closing braces than opening braces.
     */
    public int calculateBraceNesting(final String initialText) {
        String text = initialText.replaceAll("\\\\.", "_"); // Remove escaped characters.
        text = text.replaceAll("'.'", "_"); // Remove character literals.
        text = text.replaceAll("\"([^\\n]*?)\"", "_"); // Remove string literals.
        text = text.replaceAll("/\\*(?s).*?\\*/", "_"); // Remove C comments.
        text = text.replaceAll("//[^\\n]*", "_"); // Remove C++ comments.
        int braceNesting = 0;
        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (ch == '{') {
                ++braceNesting;
            } else if (ch == '}') {
                --braceNesting;
            }
        }
        return braceNesting;
    }
    
    /**
     * Returns a string corresponding to the spaces and tabs found at the
     * start of the line containing the given offset.
     */
    private String getIndentationOfLineAtOffset(int offset) {
        int lineNumber = textArea.getLineOfOffset(offset);
        return textArea.getIndentationOfLine(lineNumber);
    }
}
