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
    public boolean isElectric(char c) {
        return false;
    }
    
    @Override
    protected String calculateNewIndentation(int lineNumber) {
        final int previousNonBlankLineNumber = getPreviousNonBlankLineNumber(lineNumber);
        return (previousNonBlankLineNumber == -1) ? "" : getCurrentIndentationOfLine(previousNonBlankLineNumber);
    }
}
