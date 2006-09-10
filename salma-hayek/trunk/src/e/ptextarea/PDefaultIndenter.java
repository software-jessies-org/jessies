package e.ptextarea;

/**
 * The default indenter does no indenting.
 * Used for plain text, and languages we don't know about.
 */
public class PDefaultIndenter extends PIndenter {
    public PDefaultIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    /**
     * Ignores all characters.
     */
    public boolean isElectric(char c) {
        return false;
    }
    
    /**
     * Leaves the line as it is.
     */
    public void fixIndentationOnLine(int lineIndex) {
    }
}
