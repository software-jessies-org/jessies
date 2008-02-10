package e.ptextarea;

/**
 * The copying indenter copies the indentation from the previous non-blank line.
 * Used for languages we don't know much about.
 */
public class PCopyingIndenter extends PSimpleIndenter {
    public PCopyingIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    /**
     * Ignores all characters.
     */
    @Override
    public boolean isElectric(char c) {
        return false;
    }
    
    /**
     * Marks this indenter as being unsuitable for fixing indentation.
     * It should only be used by PNewlineInserter for computing auto-indent.
     */
    @Override
    public boolean canOnlyAutoIndent() {
        return true;
    }
    
    @Override
    protected String calculateNewIndentation(int lineNumber) {
        final int previousNonBlankLineNumber = getPreviousNonBlankLineNumber(lineNumber);
        return (previousNonBlankLineNumber == -1) ? "" : getCurrentIndentationOfLine(previousNonBlankLineNumber);
    }
}
